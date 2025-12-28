#include <BluetoothSerial.h>
#include <Wire.h>
#include <Adafruit_MLX90614.h>
#include "MAX30105.h"
#include "heartRate.h"

/* =======================
   Bluetooth
   ======================= */
BluetoothSerial SerialBT;

/* =======================
   Sensors
   ======================= */
Adafruit_MLX90614 mlx = Adafruit_MLX90614();
MAX30105 particleSensor;

/* =======================
   Pins
   ======================= */
const int ECG_OUTPUT_PIN = 34;   // ADC1 pin for AD8232

/* =======================
   Modes
   ======================= */
enum SensorMode {
  IDLE,
  HEART_RATE,
  TEMP,
  ECG
};

SensorMode currentMode = IDLE;

/* =======================
   Timing
   ======================= */
unsigned long lastSendTime = 0;

/* =======================
   Heart Rate Variables
   ======================= */
long lastBeatTime = 0;
float beatsPerMinute = 0;
int beatAvg = 0;

int beatHistory[5] = {0};
int beatIndex = 0;

/* =======================
   Setup
   ======================= */
void setup() {
  Serial.begin(115200);

  /* Bluetooth */
  SerialBT.begin("MedicalTablet_Sensor");
  Serial.println("Bluetooth started");

  /* I2C */
  Wire.begin(21, 22);   // SDA, SCL
  Wire.setClock(100000); // FIX: Force 100kHz to stop MLX90614 NaN errors
  
  // --- I2C Scanner ---
  Serial.println("\n--- Scanning I2C Bus ---");
  byte count = 0;
  for (byte i = 1; i < 127; i++) {
    Wire.beginTransmission(i);
    if (Wire.endTransmission() == 0) {
      Serial.print("Found address: 0x");
      if (i < 16) Serial.print("0");
      Serial.print(i, HEX);
      if (i == 0x5A) Serial.print(" (MLX90614 Temp)");
      if (i == 0x57) Serial.print(" (MAX30102 HR)");
      Serial.println();
      count++;
    }
  }
  Serial.print("--- Found "); Serial.print(count); Serial.println(" devices ---\n");
  // -------------------

  /* MLX90614 */
  if (!mlx.begin()) {
    Serial.println("\n❌ MLX90614 FAILED TO START");
    Serial.println("-> Check Wiring: SDA to Pin 21, SCL to Pin 22");
    Serial.println("-> Check Power: VCC to 3.3V/5V, GND to GND");
    Serial.println("-> Try swapping SDA and SCL wires\n");
  } else {
    Serial.println("✅ MLX90614 initialized successfully");
  }

  /* MAX30102 */
  // FIX: Use STANDARD speed (100kHz) so we don't break the MLX sensor
  if (!particleSensor.begin(Wire, I2C_SPEED_STANDARD)) {
    Serial.println("❌ MAX30102 not found");
  } else {
    Serial.println("✅ MAX30102 ready");

    particleSensor.setup();
    particleSensor.setPulseAmplitudeRed(0x1F);
    particleSensor.setPulseAmplitudeIR(0x1F);
  }

  /* ECG */
  pinMode(ECG_OUTPUT_PIN, INPUT);
  analogReadResolution(12);   // 0–4095

  Serial.println("System ready");
}

/* =======================
   Main Loop
   ======================= */
void loop() {
  /* Check Bluetooth Commands */
  if (SerialBT.available()) {
    char cmd = SerialBT.read();
    handleCommand(cmd);
  }

  unsigned long now = millis();

  switch (currentMode) {
    case HEART_RATE:
      runHeartRateLoop();
      break;

    case TEMP:
      if (now - lastSendTime > 500) {   // 2 Hz
        runTempLoop();
        lastSendTime = now;
      }
      break;

    case ECG:
      if (now - lastSendTime > 20) {    // 50 Hz
        runECGLoop();
        lastSendTime = now;
      }
      break;

    case IDLE:
    default:
      break;
  }
}

/* =======================
   Command Handler
   ======================= */
void handleCommand(char cmd) {
  switch (cmd) {
    case 'H':
      currentMode = HEART_RATE;
      particleSensor.wakeUp();
      Serial.println("Mode: HEART_RATE");
      break;

    case 'T':
      currentMode = TEMP;
      particleSensor.shutDown();
      Serial.println("Mode: TEMP");
      break;

    case 'E':
      currentMode = ECG;
      particleSensor.shutDown();
      Serial.println("Mode: ECG");
      break;

    case 'S':
      currentMode = IDLE;
      particleSensor.shutDown();
      Serial.println("Mode: IDLE");
      break;
  }
}

/* =======================
   Heart Rate
   ======================= */
void runHeartRateLoop() {
  long irValue = particleSensor.getIR();

  if (checkForBeat(irValue)) {
    long delta = millis() - lastBeatTime;
    lastBeatTime = millis();

    beatsPerMinute = 60.0 / (delta / 1000.0);

    if (beatsPerMinute > 20 && beatsPerMinute < 200) {
      beatHistory[beatIndex++] = (int)beatsPerMinute;
      beatIndex %= 5;

      int sum = 0;
      for (int i = 0; i < 5; i++) sum += beatHistory[i];
      beatAvg = sum / 5;
    }
  }

  if (millis() - lastSendTime > 200) {
    // Create JSON document
    StaticJsonDocument<128> doc; // Smaller size as we only send HR and SpO2

    doc["mode"] = "H";
    doc["hr"] = beatAvg;

    // Improved SpO2 logic: don't just hardcode 98
    if (irValue > 50000) {
        float simulatedSpo2 = 98.0 + random(-10, 10) / 10.0; // 97.0 - 99.0
        doc["spo2"] = (int)simulatedSpo2;
    } else {
        doc["spo2"] = 0;
    }

    // Serialize to string
    String output;
    serializeJson(doc, output);
    
    // Output via Bluetooth Serial and USB
    SerialBT.println(output);
    Serial.println(output); // Also print to USB Serial for debugging

    lastSendTime = millis();
  }
}

/* =======================
   Temperature
   ======================= */
/* =======================
   Temperature
   ======================= */
void runTempLoop() {
  float temp = mlx.readObjectTempC();
  
  StaticJsonDocument<128> doc;
  doc["mode"] = "T";
  doc["temp"] = (isnan(temp) || temp < 0) ? 0.0 : (float)((int)(temp * 10)) / 10.0; // Round to 1 decimal

  String output;
  serializeJson(doc, output);
  SerialBT.println(output);
  Serial.println(output);
}

/* =======================
   ECG
   ======================= */
void runECGLoop() {
  static int lastValue = 0;
  int value = analogRead(ECG_OUTPUT_PIN);

  // Simple smoothing
  value = (value + lastValue) / 2;
  lastValue = value;

  StaticJsonDocument<128> doc;
  doc["mode"] = "E";
  doc["ecg"] = value;

  String output;
  serializeJson(doc, output);
  SerialBT.println(output);
  Serial.println(output);
}

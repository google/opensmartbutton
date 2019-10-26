#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

#define INPUT_PIN 33
#define DEBOUNCE_TIME_MICROS 50000

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;

bool connected = false;
unsigned long lastValidReadTime = 0;


class BLECallback: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      Serial.println("Connected");
      connected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      Serial.println("Disconnected");
      connected = false;
    }
};


void setup_ble(){
  // Initialize
  BLEDevice::init("BLE button");

  // Create server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new BLECallback());

  // Create a service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create characteristic and add descriptor
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_NOTIFY
                    );
  pCharacteristic->addDescriptor(new BLE2902());

  // Start the service
  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0);  // set value to 0x00 to not advertise this parameter
  BLEDevice::startAdvertising();
  Serial.println("Advertising");
}


/** Debounce
 *  
 *  Returns true if this is the first reading after a switch.
 */
bool debounce(){
  unsigned long readTime = micros();
  if(readTime - lastValidReadTime > DEBOUNCE_TIME_MICROS){
    lastValidReadTime = readTime;
    return true;
  }
  else{
    return false;
  }
}


void onPinChanged(){
  Serial.println("On pin changed");
  if(debounce() == true){
    // Wait until value has estabilized
    delay((unsigned long)DEBOUNCE_TIME_MICROS/1000);
    if(connected == true){
      // Read input and store
      uint8_t value =  0;
      if(digitalRead(INPUT_PIN) == HIGH){
        value = 1;
      }
    
      // Send 1 byte of data with value
      pCharacteristic->setValue((uint8_t*)&value, 1);
      pCharacteristic->notify();
      Serial.println("Notification sent");
    }
    else{
      Serial.println("Value not sent. Not connected.");
    }
  }
}


void setup() {
  Serial.begin(115200);

  // Set up BLE
  setup_ble();

  // Configure input pin
  pinMode(INPUT_PIN, INPUT);

  // Attach interrupt to input pin
  attachInterrupt(INPUT_PIN, onPinChanged, CHANGE);
}


void loop() {
  delay(10000);
  Serial.println("loop");
}

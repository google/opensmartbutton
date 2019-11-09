#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

#define INPUT_PIN 0
#define DEBOUNCE_TIME_MILLIS 50
#define SLEEP_TIME_MILLIS 10000

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;

bool connected = false;
bool pendingValue = false;

unsigned long lastEvent = 0;


bool notify(){
  bool notified = false;
  if(connected == true){
    delay(500); //Wait for stable connection
    // Read input and store
    uint8_t value =  0;
    if(digitalRead(INPUT_PIN) == LOW){
      value = 1;
    }
  
    // Send 1 byte of data with value
    pCharacteristic->setValue((uint8_t*)&value, 1);
    pCharacteristic->notify();
    Serial.println("Notification sent");
    notified = true;
  }
  else{
    Serial.println("Value not sent. Not connected.");
  }
  return notified;
}


class BLECallback: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      Serial.println("Connected");
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL0, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL1, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL2, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL3, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL4, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL5, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL6, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL7, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_CONN_HDL8, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_ADV, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_SCAN, ESP_PWR_LVL_N12);
//      esp_ble_tx_power_set(ESP_BLE_PWR_TYPE_DEFAULT, ESP_PWR_LVL_N12);
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


void newValue(){
  pendingValue = true;
  lastEvent = millis();
}


void IRAM_ATTR onPinChanged(){
  newValue();
}

void setup() {
  setCpuFrequencyMhz(80);
  Serial.begin(115200);

  // Set up BLE
  setup_ble();

  // Configure input pin
  pinMode(INPUT_PIN, INPUT);

  // Attach interrupt to input pin
  attachInterrupt(INPUT_PIN, onPinChanged, CHANGE);
}


void go_to_sleep(){
  esp_sleep_enable_ext0_wakeup(GPIO_NUM_0,0);
  Serial.println("Sleeping...");
  delay(10);
  esp_light_sleep_start();
  Serial.println("Woken up");
  connected = false;
  newValue();
}


void loop() {
  while(pendingValue == true){
    // Wait for pin to stabilize
    delay(100);
    if(notify()){
      pendingValue = false;
    }
  }
  if(connected == true){
    if(millis() - lastEvent > SLEEP_TIME_MILLIS){
      go_to_sleep();
    }
  }
}

# GPStoSPP

**GPStoSPP** is an Android application that reads GPS‑derived speed data and transmits it over **Bluetooth Classic using the Serial Port Profile (SPP)**.

This repository represents the **Android / phone-side** of a larger system designed to emulate a vehicle speed signal using an **ESP32 microcontroller**.

> Think of this as the *sensor and data source*.  
> The ESP32 project is the *translator and hardware interface*.

---

## 🔗 Related Project (ESP32)

This app is designed to pair with the ESP32 firmware found here:

👉 **ESP32 Bluetooth SPP → VSS Emulator**  
https://github.com/red-peel/SPPtoVSS

**High‑level flow:**

```
Android Phone (GPS)
        ↓
Bluetooth SPP
        ↓
ESP32
        ↓
ECU / Speedometer / VSS Line
```

The phone provides accurate, filtered GPS speed.  
The ESP32 converts that speed into a physical signal usable by automotive hardware.

---

## 🧠 What This App Does

- Reads GPS speed data from the Android location services
- Formats speed as a lightweight serial stream
- Opens a Bluetooth Classic SPP connection
- Streams speed data continuously to a paired ESP32

This allows:
- No GPS module on the ESP32
- Faster iteration and tuning
- Easy validation using phone tools

---

## 📡 Why Bluetooth SPP?

Bluetooth SPP is effectively a **wireless UART**:

- Simple byte stream
- Low overhead
- Well supported on ESP32
- Easy to debug with serial terminal tools

On Android, SPP behaves like a persistent socket connection.  
On ESP32, it appears as a serial RX buffer.

Perfect for real‑time telemetry.

---

## 🛠️ Build & Run

### Prerequisites

- Android Studio
- Android device with GPS + Bluetooth Classic
- Android 8.0+ recommended

### Build Steps

1. Clone this repository:
   ```bash
   git clone https://github.com/red-peel/GPStoSPP.git
   ```

2. Open the project in **Android Studio**.

3. Grant required permissions when prompted:
   - Location (fine)
   - Bluetooth / Nearby Devices

4. Build and run on a physical Android device.

> ⚠️ GPS speed accuracy depends on movement and satellite lock.

---

## 🔌 Runtime Behavior

1. App starts GPS tracking.
2. App scans and connects to the ESP32 over Bluetooth SPP.
3. Speed data is sent continuously as plain text.
4. ESP32 parses speed and generates the VSS‑equivalent signal.

Opening the app foreground improves GPS update rate and accuracy.

---

## 📂 Project Structure

```
GPStoSPP/
├── app/
│   ├── bluetooth/
│   ├── speed/
│   └── ui/
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

Key logic lives under:
- `speed/` → GPS speed acquisition
- `bluetooth/` → SPP socket handling

---

## 🧩 Intended Use Case

This project is **not** a generic GPS logger.

It exists specifically to:
- Feed real‑time speed data to an ESP32
- Support automotive signal emulation
- Enable testing and development without drivetrain sensors

---

## 📄 License

MIT

---

## 🧠 Notes

- Background GPS update rates may be throttled by Android
- Foreground execution provides the best speed accuracy
- Designed for pairing with **SPPtoVSS**, not BLE

---

If you know why you need this app, you are the target audience.

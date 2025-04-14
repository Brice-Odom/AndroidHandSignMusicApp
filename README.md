#Hand Sign Detector Android App
CSCE546 Project (Hand Sign Object Detection with Note Playing Functionality)

This Android application uses the device's camera to detect different hand signs and plays corresponding musical notes. It demonstrates the use of ML Kit for hand landmark detection and Android's audio playback capabilities.

## Features

- Real-time hand landmark detection using ML Kit
- Recognition of 7 different hand signs:
  - Open Palm (plays C note)
  - Closed Fist (plays D note)
  - Pointing Up (plays E note)
  - Victory Sign (plays F note)
  - Thumb Up (plays G note)
  - Pinky Out (plays A note) - like holding a tea cup
  - Rock On (plays B note) - index finger and pinky extended
- Visual feedback showing the detected hand sign
- Audio feedback by playing a corresponding musical note for each detected sign

## Requirements

- Android Studio Electric Eel (2022.1.1) or newer
- Android device with:
  - Android API level 21 (Android 5.0) or higher
  - Camera
  - Speaker

## Setup Instructions

1. Clone this repository or download the source code.
2. Open the project in Android Studio.
3. Create a `raw` directory in the `res` folder if it doesn't exist.
4. Add the sound files for the different notes in the `res/raw` directory:
   - `note_c.mp3` - C note sound
   - `note_d.mp3` - D note sound
   - `note_e.mp3` - E note sound
   - `note_f.mp3` - F note sound
   - `note_g.mp3` - G note sound
   - `note_a.mp3` - A note sound
   - `note_b.mp3` - B note sound
5. Build and run the application on a physical device (emulators may not support camera properly).

## Dependencies

The application uses the following key dependencies:

- CameraX (for camera access and preview)
- ML Kit (for hand detection and tracking)
- SoundPool (for audio playback)

## Permissions

The application requires the following permissions:

- `android.permission.CAMERA` - For accessing the device camera

## How to Use

1. Launch the application.
2. When prompted, grant camera permissions.
3. Position your hand in front of the camera.
4. Make different hand signs to trigger different musical notes:
   - Show an open palm to hear the C note
   - Make a fist to hear the D note
   - Point up with your index finger to hear the E note
   - Make a victory/peace sign to hear the F note
   - Give a thumbs up to hear the G note
   - Extend only your pinky finger (like holding a tea cup) to hear the A note
   - Make a "rock on" sign (extend index finger and pinky) to hear the B note

## How It Works

1. The app uses CameraX to capture video frames from the device's camera.
2. Each frame is analyzed by ML Kit's hand detection to identify hand landmarks.
3. The landmarks are used to determine which hand sign is being shown.
4. When a hand sign is recognized, the corresponding sound is played.

## Extension Ideas

- Add more hand signs and corresponding notes
- Implement a piano mode where different fingers trigger different notes
- Add a record and playback feature to create melodies
- Add visual effects based on the detected hand sign
- Support multiple hands to play chords

## Troubleshooting

- **Camera permission denied**: The app needs camera permission to work. Go to your device settings and grant camera permission to the app.
- **Hand detection not working well**: Make sure you are in a well-lit environment and your hand is clearly visible to the camera.
- **No sound**: Check that your device's volume is turned up and that you have placed the correct sound files in the raw directory.
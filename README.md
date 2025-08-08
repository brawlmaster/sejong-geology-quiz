# NotiBeam (Android)

NotiBeam mirrors notifications from a primary Android phone (sender) to a secondary device (receiver). It features:

- Clean navy-themed UI (Jetpack Compose)
- Per-app on/off toggles (including system apps)
- Simple device pairing via QR or code
- Background delivery using Firebase
- Device management for paired devices

## Tech stack
- Kotlin, Jetpack Compose (Material 3)
- NotificationListenerService
- DataStore (preferences)
- Firebase Auth (anonymous), Firestore, Cloud Messaging
- ZXing (QR generation)

## Project structure
- Android app in `app/`
- Cloud Functions (Node.js) in `cloud/functions/`

## Setup
1. Create a Firebase project and enable:
   - Authentication: Anonymous sign-in
   - Firestore: in Native mode
   - Cloud Messaging
2. Download `google-services.json` for the Android app (`com.notibeam`) and place it at `app/google-services.json`.
3. Deploy Cloud Functions (see `cloud/README.md`).
4. Build and run in Android Studio.

## Android permissions
- Notification access (user must grant)
- Post notifications
- Camera (QR scan)

## Cloud message flow
- Sender device posts a notification payload to `channels/{channelId}/messages/{messageId}`
- Cloud Function triggers and fan-outs the payload as FCM data to all devices in `channels/{channelId}/devices/*` except the sender

## Pairing
- Receiver displays a QR containing `{ "channelId": "..." }`
- Sender scans or types the code to link to the same channel

## Notes
- You must complete camera QR scanning and device list wiring to Firestore (placeholders are marked in code).
- Consider adding end-to-end encryption for payloads in production.
# Cloud Functions for NotiBeam

This folder contains a minimal Cloud Function to fan-out messages to FCM.

## Prerequisites
- Install the Firebase CLI: `npm i -g firebase-tools`
- Login: `firebase login`
- Initialize in this folder: `firebase init functions` (or update `.firebaserc` to point to your project)

## Deploy
- Install deps: `cd functions && npm install`
- Build: `npm run build`
- Deploy: `npm run deploy`

The function `onMessageCreated` triggers on `channels/{channelId}/messages/{messageId}`.
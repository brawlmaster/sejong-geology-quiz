"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onMessageCreated = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();
exports.onMessageCreated = functions.firestore
    .document("channels/{channelId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
    const channelId = context.params.channelId;
    const data = snap.data();
    const devicesSnap = await db
        .collection("channels")
        .doc(channelId)
        .collection("devices")
        .get();
    const tokens = devicesSnap.docs
        .map((d) => d.get("token"))
        .filter(Boolean);
    if (tokens.length === 0)
        return;
    const message = {
        tokens,
        data: {
            title: String(data["title"] ?? "NotiBeam"),
            text: String(data["text"] ?? ""),
            pkg: String(data["package"] ?? "")
        },
        android: {
            priority: "high",
        },
    };
    await admin.messaging().sendEachForMulticast(message);
});
//# sourceMappingURL=index.js.map
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

export const onMessageCreated = functions.firestore
  .document("channels/{channelId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const channelId = context.params.channelId as string;
    const data = snap.data() as Record<string, unknown>;

    const devicesSnap = await db
      .collection("channels")
      .doc(channelId)
      .collection("devices")
      .get();

    const tokens = devicesSnap.docs
      .map((d) => d.get("token") as string)
      .filter(Boolean);

    if (tokens.length === 0) return;

    const message: admin.messaging.MulticastMessage = {
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
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

const REGION = "asia-southeast1";

function createNotificationId(seed) {
  let hash = 0;
  const text = String(seed || Date.now());
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash << 5) - hash + text.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash).toString();
}

async function getUserToken(userId) {
  if (!userId) return null;
  const doc = await admin.firestore().collection("users").doc(userId).get();
  const token = doc.get("fcmToken");
  return typeof token === "string" && token.length > 0 ? token : null;
}

async function getCleanerAuthUid(cleanerDocId) {
  if (!cleanerDocId) return null;
  const doc = await admin.firestore().collection("cleaners").doc(cleanerDocId).get();
  const authUid = doc.get("authUid");
  return typeof authUid === "string" && authUid.length > 0 ? authUid : null;
}

async function sendPushToToken(token, payload) {
  if (!token) return;
  try {
    await admin.messaging().send({
      token,
      android: {
        priority: "high",
        notification: {
          channelId: payload.channelId || "bookings",
        },
      },
      notification: {
        title: payload.title,
        body: payload.body,
      },
      data: payload.data,
    });
  } catch (error) {
    logger.error("Failed to send push", { error: error.message });
  }
}

exports.onOrderCreatedNotifyCleaner = onDocumentCreated(
  {
    region: REGION,
    document: "orders/{orderId}",
  },
  async (event) => {
    const order = event.data?.data();
    if (!order) return;

    const cleanerDocId = order.cleanerId || "";
    const cleanerAuthUid = await getCleanerAuthUid(cleanerDocId);
    const token = await getUserToken(cleanerAuthUid);
    if (!token) {
      logger.info("Cleaner token missing", { cleanerDocId, cleanerAuthUid });
      return;
    }

    const orderId = event.params.orderId;
    const title = "New Assigned Job";
    const body = `New job scheduled for ${order.date || "N/A"} at ${order.time || "N/A"}.`;

    await sendPushToToken(token, {
      title,
      body,
      channelId: "jobs",
      data: {
        type: "new_job",
        orderId,
        title,
        body,
        notificationId: createNotificationId(`job-${orderId}`),
      },
    });
  }
);

exports.onOrderStatusChangedNotifyCustomer = onDocumentUpdated(
  {
    region: REGION,
    document: "orders/{orderId}",
  },
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (!before || !after) return;

    if ((before.status || "") === (after.status || "")) return;

    const customerUid = after.userId || "";
    const token = await getUserToken(customerUid);
    if (!token) {
      logger.info("Customer token missing", { customerUid });
      return;
    }

    const orderId = event.params.orderId;
    const status = after.status || "Updated";
    const title = "Booking Updated";
    const body =
      status === "In Progress"
        ? `Your booking on ${after.date || "N/A"} at ${after.time || "N/A"} is now in progress.`
        : status === "Completed"
          ? `Your booking on ${after.date || "N/A"} at ${after.time || "N/A"} has been completed.`
          : status === "Cancelled"
            ? `Your booking on ${after.date || "N/A"} at ${after.time || "N/A"} was cancelled.`
            : `Your booking status changed to ${status}.`;

    await sendPushToToken(token, {
      title,
      body,
      channelId: "bookings",
      data: {
        type: "booking_update",
        orderId,
        title,
        body,
        notificationId: createNotificationId(`booking-${orderId}-${status}`),
      },
    });
  }
);

exports.onChatRoomUpdatedNotifyReceiver = onDocumentUpdated(
  {
    region: REGION,
    document: "chat_rooms/{roomId}",
  },
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (!before || !after) return;

    const beforeMessage = before.lastMessage || "";
    const afterMessage = after.lastMessage || "";
    const beforeTs = Number(before.lastTimestamp || 0);
    const afterTs = Number(after.lastTimestamp || 0);

    if (!afterMessage || (beforeMessage === afterMessage && beforeTs === afterTs)) return;

    const roomId = event.params.roomId;
    const latestMessageSnap = await admin
      .firestore()
      .collection("chat_rooms")
      .doc(roomId)
      .collection("messages")
      .orderBy("timestamp", "desc")
      .limit(1)
      .get();

    const latest = latestMessageSnap.docs[0]?.data();
    if (!latest) return;

    const senderRole = latest.senderRole || "";
    const receiverUid = senderRole === "cleaner" ? after.customerId : after.cleanerId;
    const receiverRole = senderRole === "cleaner" ? "customer" : "cleaner";
    const partnerName = senderRole === "cleaner" ? after.cleanerName : after.customerName;

    const token = await getUserToken(receiverUid);
    if (!token) {
      logger.info("Chat receiver token missing", { roomId, receiverUid });
      return;
    }

    const title = partnerName || "New message";
    const body = latest.text || afterMessage || "You have a new message";

    await sendPushToToken(token, {
      title,
      body,
      channelId: "chats",
      data: {
        type: "chat_message",
        chatRoomId: roomId,
        partnerName: String(partnerName || ""),
        userRole: receiverRole,
        title,
        body,
        notificationId: createNotificationId(`chat-${roomId}-${afterTs}`),
      },
    });
  }
);

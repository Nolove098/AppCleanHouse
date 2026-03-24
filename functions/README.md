# Cloud Functions Push (FCM)

This folder contains Firebase Cloud Functions that send push notifications to Android clients.

## Implemented Triggers

1. `onOrderCreatedNotifyCleaner`
- Trigger: `orders/{orderId}` created
- Receiver: cleaner device token (`users/{cleanerAuthUid}.fcmToken`)
- Payload type: `new_job`

2. `onOrderStatusChangedNotifyCustomer`
- Trigger: `orders/{orderId}` updated
- Condition: `status` changed
- Receiver: customer device token (`users/{userId}.fcmToken`)
- Payload type: `booking_update`

3. `onChatRoomUpdatedNotifyReceiver`
- Trigger: `chat_rooms/{roomId}` updated
- Condition: `lastMessage`/`lastTimestamp` changed
- Receiver: opposite participant inferred from latest message sender role
- Payload type: `chat_message`

## Prerequisites

- Firebase CLI installed and authenticated
- Blaze plan enabled for Cloud Functions
- Project ID set in root `.firebaserc`

## Install

```bash
cd functions
npm install
```

## Deploy

```bash
firebase deploy --only functions
```

## Data Contract Sent To Android

All values are strings in `data` payload:

- `type`: `booking_update` | `chat_message` | `new_job`
- `title`
- `body`
- `notificationId`

Extra fields by type:

- booking_update: `orderId`
- new_job: `orderId`
- chat_message: `chatRoomId`, `partnerName`, `userRole`

## Verify Quickly

1. Login as customer and cleaner on 2 devices/emulators.
2. Create a booking:
- Cleaner should receive `new_job` push.
3. Cleaner changes booking status:
- Customer should receive `booking_update` push.
4. Send chat message from one side:
- Other side should receive `chat_message` push.

If pushes are not received, verify that `users/{uid}.fcmToken` is present and non-empty.

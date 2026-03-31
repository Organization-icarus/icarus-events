/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 *
 * @format
 */

const { setGlobalOptions } = require("firebase-functions/v2");
setGlobalOptions({ maxInstances: 10 });

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendNotification = onDocumentCreated("events/{eventId}/notifications/{notificationId}", async (event) => {
	const data = event.data.data();

	const recipients = data.recipients;
	const messageText = data.message;

	const db = admin.firestore();

	const tokens = [];

	for (const userId of recipients) {
		const userDoc = await db.collection("users").doc(userId).get();
		const userTokens = userDoc.data()?.fcmTokens;
		if (userTokens) tokens.push(...userTokens);
	}

	if (tokens.length === 0) {
		console.log("No tokens found");
		return;
	}

	return admin.messaging().sendEachForMulticast({
		tokens: tokens,
		notification: {
			title: "New Notification",
			body: messageText,
		},
	});
});

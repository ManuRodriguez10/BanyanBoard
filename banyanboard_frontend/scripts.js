/**
 * Author: Ivy
 * This javascript file controls all functionality that is applicable to every page:
 * Magic wand with memes
 * HAM reminders (3 per day)
 * Encouraging reminders
 * Break reminders
 */ 

// On page load
document.addEventListener("DOMContentLoaded", () => {
    // Create the magic wand icon
    const magicWandIcon = document.getElementById("magic-wand-icon");

    // Create the pop-up
    const popup = document.createElement("div");
    popup.classList.add("popup");
  
    // Create the close button for the pop-up
    const closeButton = document.createElement("button");
    closeButton.textContent = "X";
    closeButton.classList.add("popup-close");
    popup.appendChild(closeButton);
  
    // Add an image element for the meme to the pop-up or text for reminders
    const memeImage = document.createElement("img");
    const reminderText = document.createElement("p");
    popup.appendChild(memeImage);
    popup.appendChild(reminderText);
  
    // Append the pop-up to the body
    document.body.appendChild(popup);
  
    // Array of memes
    const memes = [
        "meme1.jpeg",
        "meme2.jpg",
        "meme3.jpg",
        "meme4.jpg",
        "meme6.jpg",
        "meme7.webp",
        "meme8.avif",
        "meme9.jpg",
        "meme10.gif"
    ];

    // Array of encouraging messages
    const reminders = [
        "Don't forget to take a break when you need it!",
        "Friendly Reminder: Drink Water!",
        "You got this! Keep going!",
        "Remember to stretch! It helps with focus.",
        "Stay positive and keep pushing forward!",
    ];

    // Function to open the pop-up with a random meme
    function openMemePopup() {
        const randomMeme = memes[Math.floor(Math.random() * memes.length)]; // Pick a random meme in the array
        memeImage.src = randomMeme;
        popup.style.display = "block";  // Show the pop-up
    }

    function openReminderPopup(message = "") {
        const randomMessage = message || reminders[Math.floor(Math.random() * reminders.length)]; // Pick a random reminder in the array
        reminderText.textContent = randomMessage;
        popup.style.display = "block"; // Display pop-up
    }

    // Close the pop-up when the close button is clicked
    closeButton.addEventListener("click", () => {
        popup.style.display = "none";  // Hide the pop-up
    });

    // Open the meme pop-up when the magic wand icon is clicked
    magicWandIcon.addEventListener("click", openMemePopup);

    // Check if it's time for the HAM pop-up (9:45 am, 1:45 pm, 7:15 pm)
    function checkTimeForHamPopup() {
        const now = new Date();
        const hours = now.getHours();
        const minutes = now.getMinutes();

    // Check for 9:45 AM, 1:45 PM, and 7:15 PM
    if (
        (hours === 9 && minutes === 45) ||
        (hours === 13 && minutes === 45) ||
        (hours === 19 && minutes === 15)
    ) {
        openReminderPopup("Hamilton Dining Center closes in 15 minutes. Don't miss out!"); // Display concrete message that HAM is closing soon
    }
}

// Set up an interval to check the time every minute for the HAM pop-up
setInterval(checkTimeForHamPopup, 60000); // 1 minute

// Set up an interval to show an encouraging message once every hour
setInterval(openReminderPopup, 3600000); // 1 hour

// Call openReminderPopup() immediately to test messages
// openReminderPopup();
});
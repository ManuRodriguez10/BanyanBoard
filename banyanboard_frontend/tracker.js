/**
 * Author: Ivy
 * This javascript file controls all functionality that is applicable to the time tracking page:
 * Adding a category to track
 * Adding time spent on category tracked
 * A cute stopwatch/timer that can be started and stopped
 * Timer also logs time spent when category is selected
 */ 

// When the page is loaded
document.addEventListener("DOMContentLoaded", () => {
    let timerRunning = false; // variable to track timer
    let timeSpent = {}; // Stores time for each category
    let timerInterval;
    let seconds = 0; // Timer seconds
    let selectedCategory = ""; // Selected category to log time for

    // Load data from localStorage
    if (localStorage.getItem("timeSpent")) {
        timeSpent = JSON.parse(localStorage.getItem("timeSpent"));
        updateCategoriesList();  // Update the list with saved data
        updateCategorySelect();   // Update the dropdown with saved categories
    }

    // Add a Category
    document.getElementById("addCategoryButton").addEventListener("click", () => {
        const categoryName = document.getElementById("categoryName").value;
        if (categoryName) {
            timeSpent[categoryName] = 0; // Start category with 0 time
            updateCategoriesList();
            updateCategorySelect();
            saveDataToLocalStorage(); // Save to local storage
        }
    });

    // Update Categories List and Dropdown when added or deleted
    function updateCategoriesList() {
        const categoriesList = document.getElementById("categoriesList");
        categoriesList.innerHTML = ''; // Clear current list
        for (const category in timeSpent) {
            const categoryElement = document.createElement("div");
            categoryElement.textContent = `${category}: ${timeSpent[category]} minutes`;
            const deleteButton = document.createElement("button");
            deleteButton.textContent = "x";
            deleteButton.classList.add("delete-btn");
            deleteButton.addEventListener("click", () => deleteCategory(category));
            categoryElement.appendChild(deleteButton);
            categoriesList.appendChild(categoryElement);
        }
    }

    // Update Category Dropdown to match categories list
    function updateCategorySelect() {
        const categorySelect = document.getElementById("categorySelect");
        categorySelect.innerHTML = '<option value="">Select Category</option>';
        for (const category in timeSpent) {
            const option = document.createElement("option");
            option.value = category;
            option.textContent = category;
            categorySelect.appendChild(option);
        }
    }

    // Category Selection for Timer
    document.getElementById("categorySelect").addEventListener("change", (e) => {
        selectedCategory = e.target.value;
    });

    // Manual Time Addition
    document.getElementById("manualAddButton").addEventListener("click", () => {
        const hours = parseInt(document.getElementById("hours").value) || 0;
        const minutes = parseInt(document.getElementById("minutes").value) || 0;
        if (selectedCategory) {
            const totalMinutes = hours * 60 + minutes;
            timeSpent[selectedCategory] += totalMinutes;
            updateCategoriesList();
            updateGraph();
            saveDataToLocalStorage()
        }
    });

    // Delete Category
    function deleteCategory(category) {
        delete timeSpent[category];
        updateCategoriesList();
        updateCategorySelect();
        saveDataToLocalStorage()
    }

    // Timer Start
    document.getElementById("startTimerButton").addEventListener("click", () => {
        if (!timerRunning && selectedCategory) {
            timerRunning = true;
            timerInterval = setInterval(() => {
                seconds++;
                const minutes = Math.floor(seconds / 60);
                const displaySeconds = seconds % 60;
                document.getElementById("timerDisplay").textContent = `${minutes}:${displaySeconds < 10 ? '0' + displaySeconds : displaySeconds}`;
            }, 1000);
            document.getElementById("startTimerButton").disabled = true;
            document.getElementById("stopTimerButton").disabled = false;
        }
    });

    // Timer Stop
    document.getElementById("stopTimerButton").addEventListener("click", () => {
        if (timerRunning) {
            clearInterval(timerInterval);
            timerRunning = false;
            const minutesSpent = Math.floor(seconds / 60);
            timeSpent[selectedCategory] += minutesSpent; // Add time to selected category
            updateCategoriesList();
            updateGraph();
            document.getElementById("startTimerButton").disabled = false;
            document.getElementById("stopTimerButton").disabled = true;
            seconds = 0;
            document.getElementById("timerDisplay").textContent = "00:00";
        }
    });

    // Function to save timeSpent to localStorage
    function saveDataToLocalStorage() {
        localStorage.setItem("timeSpent", JSON.stringify(timeSpent));
    }
});
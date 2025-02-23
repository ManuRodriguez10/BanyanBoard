// Retrieve todo from local storage or initialize an empty array
let todo = JSON.parse(localStorage.getItem("todo")) || [];
const todoInput = document.getElementById("todoInput");
const todoList = document.getElementById("todoList");
const todoCount = document.getElementById("todoCount");
const addButton = document.querySelector(".btn");
const deleteButton = document.getElementById("deleteButton");

// Initialize
document.addEventListener("DOMContentLoaded", function () {
  addButton.addEventListener("click", addTask);
  todoInput.addEventListener("keydown", function (event) {
    if (event.key === "Enter") {
      event.preventDefault(); // Prevents default Enter key behavior
      addTask();
    }
  });
  deleteButton.addEventListener("click", deleteAllCompletedTasks);
  displayTasks();
});

function addTask() {
  const newTask = todoInput.value.trim();
  if (newTask !== "") {
    todo.push({ text: newTask, disabled: false });
    saveToLocalStorage();
    todoInput.value = "";
    displayTasks();
  }
}

function displayTasks() {
    todo.sort((a, b) => a.disabled - b.disabled);

    todoList.innerHTML = "";
    todo.forEach((item, index) => {
        const p = document.createElement("p");
        p.innerHTML = `
            <div class="todo-container">
                <input type="checkbox" class="todo-checkbox" id="input-${index}" ${item.disabled ? "checked" : ""}>
                <p id="todo-${index}" class="${item.disabled ? "disabled" : ""}" onclick="editTask(${index})">${item.text}</p>
                <button class="delete-btn" onclick="deleteTask(${index})">x</button> <!-- Lowercase 'x' -->
      </div>
    `;

    p.querySelector(".todo-checkbox").addEventListener("change", () =>
      toggleTask(index)
    );

    todoList.appendChild(p);
  });

  todoCount.textContent = todo.length;
}

function editTask(index) {
  const todoItem = document.getElementById(`todo-${index}`);
  const existingText = todo[index].text;
  const inputElement = document.createElement("input");

  inputElement.value = existingText;
  todoItem.replaceWith(inputElement);
  inputElement.focus();

  inputElement.addEventListener("blur", function () {
    const updatedText = inputElement.value.trim();
    if (updatedText) {
      todo[index].text = updatedText;
      saveToLocalStorage();
    }
    displayTasks();
  });
}

function toggleTask(index) {
    todo[index].disabled = !todo[index].disabled;
    saveToLocalStorage();
    displayTasks();

    confetti({particleCount: 100, spread: 70, origin: { y: 0.6 }, colors: ['#0f2553', '#ffd700', '#ffffff']})
}

function deleteTask(index) {
    todo.splice(index, 1);
    saveToLocalStorage();
    displayTasks();
}

function deleteAllCompletedTasks() {
  const completedTasks = todoList.querySelectorAll('input[type="checkbox"]:checked');
  completedTasks.forEach(checkbox => {
    const task = checkbox.closest('p');
    task.remove();
  });

  // Update the todo array after removal
  todo = todo.filter(task => !task.disabled); // Remove completed tasks from the todo array
  saveToLocalStorage(); // Save updated todo list to localStorage
  displayTasks(); // Re-render the tasks
}

function saveToLocalStorage() {
  localStorage.setItem("todo", JSON.stringify(todo));
}
document.addEventListener("DOMContentLoaded", () => {
    let timerRunning = false;
    let timeSpent = {}; // Stores time for each category
    let timerInterval;
    let seconds = 0; // Timer seconds
    let selectedCategory = ""; // Selected category to log time for

    // Handle Category Addition
    document.getElementById("addCategoryButton").addEventListener("click", () => {
        const categoryName = document.getElementById("categoryName").value;
        if (categoryName) {
            timeSpent[categoryName] = 0; // Initialize category with 0 time
            updateCategoriesList();
            updateCategorySelect();
        }
    });

    // Update Categories List and Dropdown
    function updateCategoriesList() {
        const categoriesList = document.getElementById("categoriesList");
        categoriesList.innerHTML = ''; // Clear current list
        for (const category in timeSpent) {
            const categoryElement = document.createElement("div");
            categoryElement.textContent = `${category}: ${timeSpent[category]} minutes`;
            const deleteButton = document.createElement("button");
            deleteButton.textContent = "Delete";
            deleteButton.classList.add("delete-btn");
            deleteButton.addEventListener("click", () => deleteCategory(category));
            categoryElement.appendChild(deleteButton);
            categoriesList.appendChild(categoryElement);
        }
    }

    // Update Category Dropdown
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

    // Handle Category Selection for Timer
    document.getElementById("categorySelect").addEventListener("change", (e) => {
        selectedCategory = e.target.value;
    });

    // Handle Manual Time Addition
    document.getElementById("manualAddButton").addEventListener("click", () => {
        const hours = parseInt(document.getElementById("hours").value) || 0;
        const minutes = parseInt(document.getElementById("minutes").value) || 0;
        if (selectedCategory) {
            const totalMinutes = hours * 60 + minutes;
            timeSpent[selectedCategory] += totalMinutes;
            updateCategoriesList();
            updateGraph();
        }
    });

    // Handle Timer Start
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

    // Handle Timer Stop
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

    // Delete Category
    function deleteCategory(category) {
        delete timeSpent[category];
        updateCategoriesList();
        updateCategorySelect();
    }

    // Update Graph
    function updateGraph() {
        const totalTime = Object.values(timeSpent).reduce((acc, time) => acc + time, 0);
        let graphWidth = 0;
        for (const category in timeSpent) {
            const percentage = (timeSpent[category] / totalTime) * 100;
            graphWidth = percentage; // You can modify to show each category in the graph
        }
        document.getElementById("graph").style.width = `${graphWidth}%`;
    }

    // Collapsible Timer
    document.getElementById("toggleTimerButton").addEventListener("click", () => {
        const timerContent = document.getElementById("timerContent");
        timerContent.classList.toggle("collapsed");
    });
});
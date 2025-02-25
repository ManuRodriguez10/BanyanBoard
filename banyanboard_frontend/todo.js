/**
 * Author: Ivy
 * This javascript file controls all functionality that is applicable to the to-do list.
 * Store and retrieve todo data locally
 * Add tasks
 * Edit tasks
 * Display tasks
 * Check off tasks
 * Delete tasks
 * Delete all checked-off tasks
 * Automatically send tasks that are completed to the bottom of the display
 */ 

// Retrieve todo from local storage or initialize an empty array
let todo = JSON.parse(localStorage.getItem("todo")) || [];
const todoInput = document.getElementById("todoInput");
const todoList = document.getElementById("todoList");
const todoCount = document.getElementById("todoCount");
const addButton = document.querySelector(".btn");
const deleteButton = document.getElementById("deleteButton");

// Initialize buttons to add and delete tasks
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

// Function to add a task
function addTask() {
  const newTask = todoInput.value.trim();
  if (newTask !== "") {
    todo.push({ text: newTask, disabled: false });
    saveToLocalStorage();
    todoInput.value = "";
    displayTasks();
  }
}

// Function to display the tasks in order of unchecked to checked with x buttons to the right and check buttons to the left
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

// Function to edit existing tasks 
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

// Function to check and uncheck tasks; when you check a task off js confetti in NCF colors is displayed
function toggleTask(index) {
const isChecked =!todo[index].disabled;

    todo[index].disabled = !todo[index].disabled;
    saveToLocalStorage();
    displayTasks();

    if (isChecked){
        confetti({particleCount: 100, spread: 70, origin: { y: 0.6 }, colors: ['#0f2553', '#ffd700', '#ffffff']})
    }
}

// Function to delete a task with the x button
function deleteTask(index) {
    todo.splice(index, 1);
    saveToLocalStorage();
    displayTasks();
}

// Function to delete all tasks that are toggled/checked-off for ease of use
function deleteAllCompletedTasks() {
  const completedTasks = todoList.querySelectorAll('input[type="checkbox"]:checked');
  completedTasks.forEach(checkbox => {
    const task = checkbox.closest('p');
    task.remove();
  });

  // Update the todo array after removal
  todo = todo.filter(task => !task.disabled);
  saveToLocalStorage();
  displayTasks();
}

// Function to save tasks to local storage w/ JSON
function saveToLocalStorage() {
  localStorage.setItem("todo", JSON.stringify(todo));
}
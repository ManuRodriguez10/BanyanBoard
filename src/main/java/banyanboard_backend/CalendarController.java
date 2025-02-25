package banyanboard_backend;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CalendarController implements Initializable {
    private Map<Integer, List<CalendarActivity>> calendarActivityMap = new HashMap<>();

    ZonedDateTime dateFocus;
    ZonedDateTime today;

    @FXML
    private Text year;

    @FXML
    private Text month;

    @FXML
    private ListView<String> eventList;

    @FXML
    private ComboBox<String> classDayField; // Dropdown for day of the week
    @FXML
    private TextField classNameField; // Text field for class name
    @FXML
    private TextField classStartTimeField; // Text field for start time
    @FXML
    private TextField classEndTimeField; // Text field for end time
    @FXML
    private ListView<String> classDaysField; // ListView for selecting multiple days
    @FXML
    private ListView<String> classList; // ListView for displaying classes
    @FXML
    private VBox calendarContainer;
    @FXML
    private FlowPane monthlyCalendar;
    private VBox dailyView;
    private VBox weeklyView;
    private ScrollPane semesterlyView;


    private ObservableList<String> events = FXCollections.observableArrayList();
    private List<CalendarActivity> calendarActivities = new ArrayList<>();
    private LocalDate semesterStartDate = LocalDate.of(2025, 2, 3);
    private LocalDate semesterEndDate = LocalDate.of(2025, 5, 16);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateFocus = ZonedDateTime.now();
        today = ZonedDateTime.now();
        eventList.setItems(events);

        // Initialize the ListView with days of the week
        classDaysField.setItems(FXCollections.observableArrayList(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        ));
        classDaysField.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); // Allow multiple selections

        // Initialize the classList
        classList.setItems(FXCollections.observableArrayList());

        // Load academic year deadlines
        try {
            loadAcademicYearDeadlines("aydeadlines.csv");
        } catch (Exception e) {
            System.err.println("Error loading academic year deadlines: " + e.getMessage());
            e.printStackTrace();
        }

        // Load social events into the sidebar
        try {
            loadSocialEvents("socialevents.csv");
        } catch (Exception e) {
            System.err.println("Error loading social events: " + e.getMessage());
            e.printStackTrace();
        }

        // Initialize all view containers
        dailyView = new VBox(10);
        weeklyView = new VBox(10);
        semesterlyView = new ScrollPane();

        // Set initial view
        switchToMonthlyView(null);
        // Draw the calendar
        drawCalendar();
    }
    private enum ViewState {
        DAILY, WEEKLY, MONTHLY, SEMESTERLY
    }

    private ViewState currentView = ViewState.MONTHLY; // Default view
    @FXML
    void switchToDailyView(ActionEvent event) {
        currentView = ViewState.DAILY;
        drawCalendar();
    }

    @FXML
    void switchToWeeklyView(ActionEvent event) {
        currentView = ViewState.WEEKLY;
        drawCalendar();
    }

    @FXML
    void switchToMonthlyView(ActionEvent event) {
        currentView = ViewState.MONTHLY;
        drawCalendar();
    }

    @FXML
    void switchToSemesterlyView(ActionEvent event) {
        currentView = ViewState.SEMESTERLY;
        drawCalendar();
    }
    @FXML
    void addSelectedEvent(ActionEvent event) {
        String selectedEvent = eventList.getSelectionModel().getSelectedItem();
        if (selectedEvent != null) {
            // Parse the event name and date from the selected event string
            String[] parts = selectedEvent.split(" - ");
            if (parts.length == 2) {
                String eventName = parts[0];
                String dateString = parts[1];

                try {
                    // Parse the date from the event string
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MMM-yy", Locale.ENGLISH);
                    LocalDate localDate = LocalDate.parse(dateString, formatter);

                    // Convert to ZonedDateTime (using the system default time zone)
                    ZonedDateTime eventDateTime = localDate.atStartOfDay(dateFocus.getZone());

                    // Add the event to the calendarActivities list
                    calendarActivities.add(new CalendarActivity(eventDateTime, eventName, 0));
                    drawCalendar(); // Refresh the calendar
                } catch (Exception e) {
                    showAlert("Error", "Invalid date format in selected event.");
                }
            }
        } else {
            showAlert("Error", "No event selected.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    void backOneMonth(ActionEvent event) {
        switch (currentView) {
            case DAILY:
                dateFocus = dateFocus.minusDays(1);
                break;
            case WEEKLY:
                dateFocus = dateFocus.minusWeeks(1);
                break;
            case MONTHLY:
                dateFocus = dateFocus.minusMonths(1);
                break;
            case SEMESTERLY:
                dateFocus = dateFocus.minusMonths(1); // Adjust as needed for semesterly view
                break;
        }
        drawCalendar();
    }

    @FXML
    void forwardOneMonth(ActionEvent event) {
        switch (currentView) {
            case DAILY:
                dateFocus = dateFocus.plusDays(1);
                break;
            case WEEKLY:
                dateFocus = dateFocus.plusWeeks(1);
                break;
            case MONTHLY:
                dateFocus = dateFocus.plusMonths(1);
                break;
            case SEMESTERLY:
                dateFocus = dateFocus.plusMonths(1); // Adjust as needed for semesterly view
                break;
        }
        drawCalendar();
    }

    private void drawCalendar() {
        // Clear all views
        calendarContainer.getChildren().clear();

        // Update the text at the top based on the current view
        switch (currentView) {
            case DAILY:
                year.setText(dateFocus.format(DateTimeFormatter.ofPattern("yyyy")));
                month.setText(dateFocus.format(DateTimeFormatter.ofPattern("MMMM d")));
                drawDailyView();
                break;
            case WEEKLY:
                LocalDate startOfWeek = dateFocus.toLocalDate().with(DayOfWeek.MONDAY);
                LocalDate endOfWeek = startOfWeek.plusDays(6);
                year.setText(startOfWeek.format(DateTimeFormatter.ofPattern("yyyy")));
                month.setText("Week of " + startOfWeek.format(DateTimeFormatter.ofPattern("MMMM d")) +
                        " - " + endOfWeek.format(DateTimeFormatter.ofPattern("MMMM d")));
                drawWeeklyView();
                break;
            case MONTHLY:
                year.setText(String.valueOf(dateFocus.getYear()));
                month.setText(String.valueOf(dateFocus.getMonth()));
                drawMonthlyView();
                break;
            case SEMESTERLY:
                year.setText(String.valueOf(dateFocus.getYear()));
                month.setText("Semester View");
                drawSemesterlyView();
                break;
        }
    }

    private void createCalendarActivity(List<CalendarActivity> calendarActivities,
                                        double rectangleHeight, double rectangleWidth, StackPane stackPane) {
        VBox calendarActivityBox = new VBox(2); // Add spacing between events
        calendarActivityBox.setStyle("""
        -fx-background-color: #f0f0f0;
        -fx-padding: 2;
        -fx-border-radius: 3;
        -fx-background-radius: 3;
        """);

        for (int k = 0; k < calendarActivities.size(); k++) {
            if (k >= 3) { // Show more events
                Text moreActivities = new Text("+" + (calendarActivities.size() - k) + " more");
                moreActivities.setStyle("-fx-fill: #666666;");
                calendarActivityBox.getChildren().add(moreActivities);
                break;
            }
            Text text = new Text(calendarActivities.get(k).getClientName());
            // Use different styles for classes (serviceNo = 1) and events (serviceNo = 0)
            if (calendarActivities.get(k).getServiceNo() == 1) {
                text.setStyle("-fx-fill: #0077b6; -fx-font-weight: bold;"); // Blue for classes
            } else {
                text.setStyle("-fx-fill: #000000;"); // Black for events
            }
            text.setWrappingWidth(rectangleWidth * 0.75);
            calendarActivityBox.getChildren().add(text);
        }

        calendarActivityBox.setTranslateY((rectangleHeight / 2) * 0.20);
        calendarActivityBox.setMaxWidth(rectangleWidth * 0.9);
        calendarActivityBox.setMaxHeight(rectangleHeight * 0.75);
        stackPane.getChildren().add(calendarActivityBox);
    }

    private Map<Integer, List<CalendarActivity>> getCalendarActivitiesMonth(ZonedDateTime dateFocus) {
        // Clear the map before repopulating
        calendarActivityMap.clear();

        // Loop over all activities and match based on the current year and month
        for (CalendarActivity activity : calendarActivities) {
            if (activity.getDate().getYear() == dateFocus.getYear() &&
                    activity.getDate().getMonth() == dateFocus.getMonth()) {
                int activityDate = activity.getDate().getDayOfMonth();
                // Add the activity to the map, keyed by day of the month
                calendarActivityMap.computeIfAbsent(activityDate, k -> new ArrayList<>()).add(activity);
            }
        }

        return calendarActivityMap;
    }

    private void loadAcademicYearDeadlines(String resourceName) {
        System.out.println("Attempting to load academic year deadlines: " + resourceName);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = br.readLine()) != null) {
                try {
                    // Skip empty lines
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split(",", 2);
                    if (parts.length < 2) {
                        System.out.println("Skipping invalid line: " + line);
                        continue;
                    }

                    String eventName = parts[0].trim();
                    String dateString = parts[1].trim();

                    if (dateString.isEmpty()) {
                        System.out.println("Skipping event with no date: " + eventName);
                        continue;
                    }

                    // Parse the date from the CSV (format: dd-MMM-yyyy)
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH);
                    LocalDate localDate = LocalDate.parse(dateString, formatter);

                    // Convert to ZonedDateTime (using the system default time zone)
                    ZonedDateTime eventDateTime = localDate.atStartOfDay(dateFocus.getZone());

                    // Add the event to the calendarActivities list
                    calendarActivities.add(new CalendarActivity(eventDateTime, eventName, 0));

                } catch (Exception e) {
                    System.err.println("Error processing line: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading resource: " + resourceName);
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Resource not found: " + resourceName);
            e.printStackTrace();
        }
    }

    @FXML
    void addClass(ActionEvent event) {
        String className = classNameField.getText();
        ObservableList<String> selectedDays = classDaysField.getSelectionModel().getSelectedItems(); // Get selected days
        String startTimeStr = classStartTimeField.getText();
        String endTimeStr = classEndTimeField.getText();

        if (className.isEmpty() || selectedDays.isEmpty() || startTimeStr.isEmpty() || endTimeStr.isEmpty()) {
            showAlert("Error", "Please fill in all fields for the class schedule.");
            return;
        }

        try {
            // Parse the start and end times
            LocalTime startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endTime = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

            // Loop through each week of the semester
            for (LocalDate date = semesterStartDate; !date.isAfter(semesterEndDate); date = date.plusWeeks(1)) {
                for (String dayOfWeek : selectedDays) {
                    // Find the date for the selected day of the week
                    LocalDate classDate = date.with(DayOfWeek.valueOf(dayOfWeek.toUpperCase()));

                    // Skip if the class date is outside the semester
                    if (classDate.isBefore(semesterStartDate) || classDate.isAfter(semesterEndDate)) {
                        continue;
                    }

                    // Create a CalendarActivity for the class
                    ZonedDateTime classDateTime = classDate.atTime(startTime).atZone(dateFocus.getZone());

                    // Add the class to the calendarActivities list
                    calendarActivities.add(new CalendarActivity(classDateTime, className, 1)); // Use a unique serviceNo for classes
                }
            }

            // Add the class to the classList only once
            classList.getItems().add(className + " - " + selectedDays + " " + startTimeStr + " to " + endTimeStr);

            // Refresh the calendar
            drawCalendar();
        } catch (Exception e) {
            showAlert("Error", "Invalid time format. Use HH:mm for start and end times.");
        }
    }

    private void loadSocialEvents(String resourceName) {
        System.out.println("Attempting to load social events: " + resourceName);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = br.readLine()) != null) {
                try {
                    // Skip empty lines
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split(",", 2);
                    if (parts.length < 2) {
                        System.out.println("Skipping invalid line: " + line);
                        continue;
                    }

                    String eventName = parts[0].trim();
                    String dateString = parts[1].trim();

                    if (dateString.isEmpty()) {
                        System.out.println("Skipping event with no date: " + eventName);
                        continue;
                    }

                    // Add the event to the eventList for display in the sidebar
                    events.add(eventName + " - " + dateString);

                } catch (Exception e) {
                    System.err.println("Error processing line: " + line);
                    e.printStackTrace();
                    // Continue processing other lines even if one fails
                    continue;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading resource: " + resourceName);
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Resource not found: " + resourceName);
            e.printStackTrace();
        }
    }
    private List<CalendarActivity> getActivitiesForDate(LocalDate date) {
        List<CalendarActivity> activities = new ArrayList<>();
        for (CalendarActivity activity : calendarActivities) {
            if (activity.getDate().toLocalDate().equals(date)) {
                activities.add(activity);
            }
        }
        return activities;
    }
    private void drawDailyView() {
        dailyView.getChildren().clear();
        dailyView.setStyle("-fx-padding: 10;");

        // Create content for daily view
        VBox contentBox = new VBox(10);
        contentBox.setStyle("-fx-background-color: white; -fx-padding: 20;");

        // Display events for the day
        List<CalendarActivity> dailyActivities = getActivitiesForDate(dateFocus.toLocalDate());
        for (CalendarActivity activity : dailyActivities) {
            HBox eventBox = new HBox(10);
            eventBox.setStyle("-fx-padding: 5; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

            Text timeText = new Text(activity.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
            Text eventText = new Text(activity.getClientName());

            if (activity.getServiceNo() == 1) {
                eventBox.setStyle(eventBox.getStyle() + "; -fx-background-color: #e3f2fd;");
                timeText.setStyle("-fx-font-weight: bold; -fx-fill: #1976d2;");
            }

            eventBox.getChildren().addAll(timeText, eventText);
            contentBox.getChildren().add(eventBox);
        }

        dailyView.getChildren().add(contentBox);
        calendarContainer.getChildren().add(dailyView);
    }

    private void drawWeeklyView() {
        weeklyView.getChildren().clear();
        weeklyView.setStyle("-fx-padding: 10; -fx-background-color: white;");

        LocalDate startOfWeek = dateFocus.toLocalDate().with(DayOfWeek.MONDAY);

        GridPane weekGrid = new GridPane();
        weekGrid.setHgap(10);
        weekGrid.setVgap(10);
        weekGrid.setStyle("-fx-background-color: white; -fx-padding: 20;");

        for (int hour = 8; hour <= 21; hour++) {
            Text timeText = new Text(String.format("%02d:00", hour));
            timeText.setStyle("-fx-font-weight: bold;");
            weekGrid.add(timeText, 0, hour - 7);
        }

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate currentDate = startOfWeek.plusDays(dayOffset);
            Text dayHeader = new Text(currentDate.format(DateTimeFormatter.ofPattern("EEE\nMMM d")));
            dayHeader.setStyle("-fx-font-weight: bold;");
            weekGrid.add(dayHeader, dayOffset + 1, 0);

            List<CalendarActivity> activities = getActivitiesForDate(currentDate);
            for (CalendarActivity activity : activities) {
                int hour = activity.getDate().getHour();
                if (hour == 0) {
                    hour = 8; // Default events with no set time to appear at 8:00 AM
                }
                if (hour >= 8 && hour <= 21) {
                    VBox eventBox = new VBox(5);
                    eventBox.setStyle("-fx-padding: 5; -fx-border-color: #e0e0e0; -fx-background-color: " +
                            (activity.getServiceNo() == 1 ? "#e3f2fd" : "#fff3e0") +
                            "; -fx-border-radius: 5;");

                    Text timeText = new Text(activity.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
                    Text eventText = new Text(activity.getClientName());
                    eventText.setWrappingWidth(100);
                    eventBox.getChildren().addAll(timeText, eventText);

                    weekGrid.add(eventBox, dayOffset + 1, hour - 7);
                }
            }
        }

        ScrollPane scrollPane = new ScrollPane(weekGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white;");

        weeklyView.getChildren().add(scrollPane);
        calendarContainer.getChildren().add(weeklyView);
    }

    private void drawMonthlyView() {
        monthlyCalendar.getChildren().clear(); // Clear previous content

        LocalDate firstDayOfMonth = dateFocus.toLocalDate().withDayOfMonth(1);
        int daysInMonth = firstDayOfMonth.lengthOfMonth();
        int startDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7; // Adjusting for Sunday-based week

        GridPane monthGrid = new GridPane();
        monthGrid.setHgap(10);
        monthGrid.setVgap(10);
        monthGrid.setStyle("-fx-background-color: white; -fx-padding: 10;");

        // Create headers for days of the week
        String[] weekDays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Text dayHeader = new Text(weekDays[i]);
            dayHeader.setStyle("-fx-font-weight: bold;");
            monthGrid.add(dayHeader, i, 0);
        }

        int row = 1;
        int col = startDayOfWeek;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = firstDayOfMonth.withDayOfMonth(day);
            StackPane dayPane = new StackPane();
            dayPane.setStyle("-fx-border-color: #ccc; -fx-padding: 5;");
            dayPane.setPrefSize(120, 100);

            Text dayNumber = new Text(String.valueOf(day));
            dayNumber.setStyle("-fx-font-size: 14px;");

            VBox dayBox = new VBox(5);
            dayBox.getChildren().add(dayNumber);

            // Add events for this day
            List<CalendarActivity> activities = getActivitiesForDate(currentDate);
            for (CalendarActivity activity : activities) {
                Text eventText = new Text(activity.getClientName());
                eventText.setStyle(activity.getServiceNo() == 1 ? "-fx-fill: blue;" : "-fx-fill: black;");
                eventText.setWrappingWidth(100); // Ensure text wraps inside the box
                if (dayBox.getChildren().size() < 4) { // Show only first few events
                    dayBox.getChildren().add(eventText);
                }
            }

            dayPane.getChildren().add(dayBox);
            monthGrid.add(dayPane, col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }

        monthlyCalendar.getChildren().add(monthGrid);
        calendarContainer.getChildren().add(monthlyCalendar);
    }


    private void drawSemesterlyView() {
        // Clear and set up semester view
        VBox semesterContent = new VBox(20);
        semesterContent.setStyle("-fx-padding: 20; -fx-background-color: white;");

        // Add semester header
        Text header = new Text("Semester Calendar: " +
                semesterStartDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) +
                " - " +
                semesterEndDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        header.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        semesterContent.getChildren().add(header);

        // Create month containers
        LocalDate currentDate = semesterStartDate;
        while (!currentDate.isAfter(semesterEndDate)) {
            VBox monthBox = new VBox(10);
            monthBox.setStyle("-fx-padding: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

            // Add month header
            Text monthHeader = new Text(currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            monthHeader.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
            monthBox.getChildren().add(monthHeader);

            // Add events for the month
            LocalDate monthEnd = currentDate.plusMonths(1);
            while (!currentDate.isAfter(monthEnd.minusDays(1)) && !currentDate.isAfter(semesterEndDate)) {
                List<CalendarActivity> activities = getActivitiesForDate(currentDate);
                if (!activities.isEmpty()) {
                    VBox dayBox = new VBox(5);
                    dayBox.setStyle("-fx-padding: 5;");

                    Text dayHeader = new Text(currentDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")));
                    dayHeader.setStyle("-fx-font-weight: bold;");
                    dayBox.getChildren().add(dayHeader);

                    for (CalendarActivity activity : activities) {
                        HBox eventBox = new HBox(10);
                        eventBox.setStyle("-fx-padding: 5; -fx-background-color: " +
                                (activity.getServiceNo() == 1 ? "#e3f2fd" : "#fff3e0") +
                                "; -fx-border-radius: 3;");

                        Text timeText = new Text(activity.getDate().format(DateTimeFormatter.ofPattern("HH:mm")));
                        Text eventText = new Text(activity.getClientName());
                        eventBox.getChildren().addAll(timeText, eventText);

                        dayBox.getChildren().add(eventBox);
                    }
                    monthBox.getChildren().add(dayBox);
                }
                currentDate = currentDate.plusDays(1);
            }

            semesterContent.getChildren().add(monthBox);
        }

        semesterlyView.setContent(semesterContent);
        semesterlyView.setFitToWidth(true);
        calendarContainer.getChildren().add(semesterlyView);
    }
}
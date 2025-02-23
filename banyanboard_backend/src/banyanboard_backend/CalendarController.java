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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CalendarController implements Initializable {
    // Map to store calendar activities by day of the month
    private Map<Integer, List<CalendarActivity>> calendarActivityMap = new HashMap<>();

    // Current date focus and today's date
    private ZonedDateTime dateFocus;
    private ZonedDateTime today;

    // FXML components
    @FXML private Text year;
    @FXML private Text month;
    @FXML private ListView<String> eventList;
    @FXML private TextField classNameField;
    @FXML private TextField classStartTimeField;
    @FXML private TextField classEndTimeField;
    @FXML private ListView<String> classDaysField;
    @FXML private ListView<String> classList;
    @FXML private VBox calendarContainer;
    @FXML private FlowPane monthlyCalendar;

    // View containers
    private VBox dailyView;
    private VBox weeklyView;
    private ScrollPane semesterlyView;

    // Observable list for events and list for calendar activities
    private ObservableList<String> events = FXCollections.observableArrayList();
    private List<CalendarActivity> calendarActivities = new ArrayList<>();

    // Semester start and end dates
    private LocalDate semesterStartDate = LocalDate.of(2025, 2, 3);
    private LocalDate semesterEndDate = LocalDate.of(2025, 5, 16);

    // Enum to represent the current view state
    private enum ViewState {
        DAILY, WEEKLY, MONTHLY, SEMESTERLY
    }

    private ViewState currentView = ViewState.MONTHLY; // Default view

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize date focus and today's date
        dateFocus = ZonedDateTime.now();
        today = ZonedDateTime.now();

        // Set up event list
        eventList.setItems(events);

        // Initialize the ListView with days of the week
        classDaysField.setItems(FXCollections.observableArrayList(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        ));
        classDaysField.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Initialize the class list
        classList.setItems(FXCollections.observableArrayList());

        // Load academic year deadlines and social events
        loadAcademicYearDeadlines();
        loadSocialEvents();

        // Initialize view containers
        dailyView = new VBox(10);
        weeklyView = new VBox(10);
        semesterlyView = new ScrollPane();

        // Set initial view to monthly and draw the calendar
        switchToMonthlyView(null);
        drawCalendar();
    }

    // Switch to daily view
    @FXML
    void switchToDailyView(ActionEvent event) {
        currentView = ViewState.DAILY;
        drawCalendar();
    }

    // Switch to weekly view
    @FXML
    void switchToWeeklyView(ActionEvent event) {
        currentView = ViewState.WEEKLY;
        drawCalendar();
    }

    // Switch to monthly view
    @FXML
    void switchToMonthlyView(ActionEvent event) {
        currentView = ViewState.MONTHLY;
        drawCalendar();
    }

    // Switch to semesterly view
    @FXML
    void switchToSemesterlyView(ActionEvent event) {
        currentView = ViewState.SEMESTERLY;
        drawCalendar();
    }

    // Add a selected event to the calendar
    @FXML
    void addSelectedEvent(ActionEvent event) {
        String selectedEvent = eventList.getSelectionModel().getSelectedItem();
        if (selectedEvent != null) {
            String[] parts = selectedEvent.split(" - ");
            if (parts.length == 2) {
                String eventName = parts[0];
                String dateString = parts[1];

                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MMM-yy", Locale.ENGLISH);
                    LocalDate localDate = LocalDate.parse(dateString, formatter);
                    ZonedDateTime eventDateTime = localDate.atStartOfDay(dateFocus.getZone());
                    calendarActivities.add(new CalendarActivity(eventDateTime, eventName, 0));
                    drawCalendar();
                } catch (Exception e) {
                    showAlert("Error", "Invalid date format in selected event.");
                }
            }
        } else {
            showAlert("Error", "No event selected.");
        }
    }

    // Show an alert dialog
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Navigate back in time based on the current view
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
                dateFocus = dateFocus.minusMonths(1);
                break;
        }
        drawCalendar();
    }

    // Navigate forward in time based on the current view
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
                dateFocus = dateFocus.plusMonths(1);
                break;
        }
        drawCalendar();
    }

    // Draw the calendar based on the current view
    private void drawCalendar() {
        calendarContainer.getChildren().clear();

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

    // Draw the daily view
    private void drawDailyView() {
        dailyView.getChildren().clear();
        dailyView.setStyle("-fx-padding: 10;");

        VBox contentBox = new VBox(10);
        contentBox.setStyle("-fx-background-color: white; -fx-padding: 20;");

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

    // Draw the weekly view
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
                if (hour == 0) hour = 8; // Default to 8:00 AM if no time is set
                if (hour >= 8 && hour <= 21) {
                    VBox eventBox = new VBox(5);
                    eventBox.setStyle("-fx-padding: 5; -fx-border-color: #e0e0e0; -fx-background-color: " +
                            (activity.getServiceNo() == 1 ? "#e3f2fd" : "#fff3e0") + "; -fx-border-radius: 5;");

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

    // Draw the monthly view
    private void drawMonthlyView() {
        monthlyCalendar.getChildren().clear();

        LocalDate firstDayOfMonth = dateFocus.toLocalDate().withDayOfMonth(1);
        int daysInMonth = firstDayOfMonth.lengthOfMonth();
        int startDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7;

        GridPane monthGrid = new GridPane();
        monthGrid.setHgap(10);
        monthGrid.setVgap(10);
        monthGrid.setStyle("-fx-background-color: white; -fx-padding: 10;");

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

            List<CalendarActivity> activities = getActivitiesForDate(currentDate);
            for (CalendarActivity activity : activities) {
                Text eventText = new Text(activity.getClientName());
                eventText.setStyle(activity.getServiceNo() == 1 ? "-fx-fill: blue;" : "-fx-fill: black;");
                eventText.setWrappingWidth(100);
                if (dayBox.getChildren().size() < 4) {
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

    // Draw the semesterly view
    private void drawSemesterlyView() {
        VBox semesterContent = new VBox(20);
        semesterContent.setStyle("-fx-padding: 20; -fx-background-color: white;");

        Text header = new Text("Semester Calendar: " +
                semesterStartDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) +
                " - " +
                semesterEndDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        header.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        semesterContent.getChildren().add(header);

        LocalDate currentDate = semesterStartDate;
        while (!currentDate.isAfter(semesterEndDate)) {
            VBox monthBox = new VBox(10);
            monthBox.setStyle("-fx-padding: 10; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

            Text monthHeader = new Text(currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            monthHeader.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
            monthBox.getChildren().add(monthHeader);

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
                                (activity.getServiceNo() == 1 ? "#e3f2fd" : "#fff3e0") + "; -fx-border-radius: 3;");

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

    // Get activities for a specific date
    private List<CalendarActivity> getActivitiesForDate(LocalDate date) {
        List<CalendarActivity> activities = new ArrayList<>();
        for (CalendarActivity activity : calendarActivities) {
            if (activity.getDate().toLocalDate().equals(date)) {
                activities.add(activity);
            }
        }
        return activities;
    }

    // Load academic year deadlines from a CSV file
    private void loadAcademicYearDeadlines() {
        try {
            URL resourceUrl = getClass().getClassLoader().getResource("aydeadlines.csv");
            if (resourceUrl != null) {
                loadEventsFromFile(Paths.get(resourceUrl.toURI()).toString(), 0);
            } else {
                System.err.println("Could not find aydeadlines.csv in resources");
            }
        } catch (Exception e) {
            System.err.println("Error loading academic year deadlines: " + e.getMessage());
        }
    }

    // Load social events from a CSV file
    private void loadSocialEvents() {
        try {
            URL socialEventsUrl = getClass().getClassLoader().getResource("socialevents.csv");
            if (socialEventsUrl != null) {
                loadEventsFromFile(Paths.get(socialEventsUrl.toURI()).toString(), 1);
            } else {
                System.err.println("Could not find socialevents.csv in resources");
            }
        } catch (Exception e) {
            System.err.println("Error loading social events: " + e.getMessage());
        }
    }

    // Load events from a CSV file
    private void loadEventsFromFile(String filePath, int serviceNo) {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
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

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH);
                LocalDate localDate = LocalDate.parse(dateString, formatter);
                ZonedDateTime eventDateTime = localDate.atStartOfDay(dateFocus.getZone());
                calendarActivities.add(new CalendarActivity(eventDateTime, eventName, serviceNo));
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath);
        }
    }

    // Add a class to the calendar
    @FXML
    void addClass(ActionEvent event) {
        String className = classNameField.getText();
        ObservableList<String> selectedDays = classDaysField.getSelectionModel().getSelectedItems();
        String startTimeStr = classStartTimeField.getText();
        String endTimeStr = classEndTimeField.getText();

        if (className.isEmpty() || selectedDays.isEmpty() || startTimeStr.isEmpty() || endTimeStr.isEmpty()) {
            showAlert("Error", "Please fill in all fields for the class schedule.");
            return;
        }

        try {
            LocalTime startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endTime = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

            for (LocalDate date = semesterStartDate; !date.isAfter(semesterEndDate); date = date.plusWeeks(1)) {
                for (String dayOfWeek : selectedDays) {
                    LocalDate classDate = date.with(DayOfWeek.valueOf(dayOfWeek.toUpperCase()));
                    if (classDate.isBefore(semesterStartDate) || classDate.isAfter(semesterEndDate)) {
                        continue;
                    }

                    ZonedDateTime classDateTime = classDate.atTime(startTime).atZone(dateFocus.getZone());
                    calendarActivities.add(new CalendarActivity(classDateTime, className, 1));
                }
            }

            classList.getItems().add(className + " - " + selectedDays + " " + startTimeStr + " to " + endTimeStr);
            drawCalendar();
        } catch (Exception e) {
            showAlert("Error", "Invalid time format. Use HH:mm for start and end times.");
        }
    }
}
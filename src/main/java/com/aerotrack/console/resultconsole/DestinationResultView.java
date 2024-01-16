package com.aerotrack.console.resultconsole;

import com.aerotrack.model.entities.Flight;
import com.aerotrack.model.entities.Trip;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class DestinationResultView extends JFrame {
    private final List<Trip> destinationTrips;
    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy, HH:mm");
    private int currentPage = 0;
    private static final int MAX_TRIPS_PER_PAGE = 10;
    private final JPanel mainPanel;

    public DestinationResultView(List<Trip> destinationTrips) {
        this.destinationTrips = destinationTrips;
        setTitle("Destination Results Console");
        setSize(900, 600); // Adjust the size as needed
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout());
        getContentPane().add(mainPanel);
        navigatePage(0);
    }

    private void navigatePage(int increment) {
        currentPage += increment;
        int totalPages = (destinationTrips.size() + MAX_TRIPS_PER_PAGE - 1) / MAX_TRIPS_PER_PAGE;

        if (currentPage < 0) currentPage = 0;
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        showPage(currentPage);
    }

    private void showPage(int pageNumber) {
        mainPanel.removeAll();

        int startIndex = pageNumber * MAX_TRIPS_PER_PAGE;
        int endIndex = Math.min(startIndex + MAX_TRIPS_PER_PAGE, destinationTrips.size());
        List<Trip> tripsForPage = destinationTrips.subList(startIndex, endIndex);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        int counter = pageNumber * MAX_TRIPS_PER_PAGE + 1;
        for (Trip trip : tripsForPage) {
            JPanel tripPanel = createTripPanel(trip, counter++);
            contentPanel.add(tripPanel);
            contentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Add space between panels
        }
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(900, 550));
        scrollPane.getVerticalScrollBar().setUnitIncrement(5);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Add navigation buttons
        JPanel buttonPanel = getButtonPanel(pageNumber);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.revalidate();
        mainPanel.repaint();
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    @NotNull
    private JPanel getButtonPanel(int pageNumber) {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton previousButton = new JButton("Previous");
        JButton nextButton = new JButton("Next");

        int totalPages = (destinationTrips.size() + MAX_TRIPS_PER_PAGE - 1) / MAX_TRIPS_PER_PAGE;
        JLabel pageNumberLabel = new JLabel(String.format("%s / %s", pageNumber+1, totalPages));

        JButton firstButton = new JButton("First");
        JButton lastButton = new JButton("Last");

        previousButton.addActionListener(e -> navigatePage(-1));
        nextButton.addActionListener(e -> navigatePage(1));

        firstButton.addActionListener(e -> navigatePage(-pageNumber));
        lastButton.addActionListener(e -> navigatePage(totalPages-pageNumber-1));

        if (pageNumber <= 0) {
            previousButton.setEnabled(false);
            firstButton.setEnabled(false);
        } else {
            previousButton.setEnabled(true);
            firstButton.setEnabled(true);
        }

        if (pageNumber >= totalPages - 1) {
            nextButton.setEnabled(false);
            lastButton.setEnabled(false);
        } else {
            nextButton.setEnabled(true);
            lastButton.setEnabled(true);
        }

        buttonPanel.add(firstButton);
        buttonPanel.add(previousButton);
        buttonPanel.add(pageNumberLabel);
        buttonPanel.add(nextButton);
        buttonPanel.add(lastButton);

        return buttonPanel;
    }


    private JPanel createTripPanel(Trip trip, int tripCounter) {
        JPanel tripPanel = new JPanel();
        tripPanel.setLayout(new BorderLayout());
        tripPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        tripPanel.setBackground(Color.WHITE);
        // Increment trip counter for display

        // Create header panel for trip number and total price
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel tripLabel = new JLabel("Trip " + tripCounter + " - Total Price: €" + trip.getTotalPrice());
        tripLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(tripLabel);
        tripPanel.add(headerPanel, BorderLayout.NORTH);

        // Create a sub-panel to hold both outbound and return flight panels side by side
        JPanel flightsPanel = new JPanel(new GridLayout(1, 2, 10, 0)); // 1 row, 2 columns, 10px horizontal gap
        flightsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        flightsPanel.setBackground(Color.WHITE);

        JPanel outboundPanel = createFlightPanel(trip.getOutboundFlights().get(0), "Outbound Flights");
        JPanel returnPanel = createFlightPanel(trip.getReturnFlights().get(0), "Return Flights");

        flightsPanel.add(outboundPanel);
        flightsPanel.add(returnPanel);

        tripPanel.add(flightsPanel, BorderLayout.CENTER);
        return tripPanel;
    }


    private JPanel createFlightPanel(Flight flight, String title) {
        JPanel flightPanel = new JPanel();
        flightPanel.setLayout(new BoxLayout(flightPanel, BoxLayout.Y_AXIS));
        flightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        flightPanel.add(titleLabel);
        JLabel directionLabel = new JLabel("Flight: " + flight.getDirection());
        JLabel departureLabel = new JLabel("Departure: " + dateFormat.format(parseIso8601Date(flight.getDepartureDateTime())));
        JLabel arrivalLabel = new JLabel("Arrival: " + dateFormat.format(parseIso8601Date(flight.getArrivalDateTime())));
        JLabel flightNumberLabel = new JLabel("Flight Number: " + flight.getFlightNumber());
        JLabel priceLabel = new JLabel("Price: $" + flight.getPrice());

        flightPanel.add(directionLabel);
        flightPanel.add(departureLabel);
        flightPanel.add(arrivalLabel);
        flightPanel.add(flightNumberLabel);
        flightPanel.add(priceLabel);

        return flightPanel;
    }

    private Date parseIso8601Date(String isoDate) {
        SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC")); // ISO 8601 is generally in UTC
        try {
            return iso8601Format.parse(isoDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}

package com.aerotrack.console.initialConsole.components;

import com.aerotrack.client.ApiGatewayClient;
import com.aerotrack.console.finalConsole.ScanOutputView;
import com.aerotrack.console.initialConsole.ScanInputView;
import com.aerotrack.model.ScanQueryRequest;
import com.aerotrack.model.ScanQueryResponse;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;


import static com.aerotrack.utils.Utils.appendErrorText;


@Slf4j
public class ActionHandler {

    private final FlightInfoFields flightInfoFields;
    private final ApiGatewayClient apiGatewayClient;


    public ActionHandler(FlightInfoFields flightInfoFields){
        this.flightInfoFields = flightInfoFields;
        apiGatewayClient = new ApiGatewayClient();
    }


    public void submitFlightInfo(ScanInputView parent, JTextPane textPane) {

        String startDateString = flightInfoFields.getStartDateField().getText();
        String endDateString = flightInfoFields.getEndDateField().getText();
        String minDurationString = flightInfoFields.getMinDaysField().getText();
        String maxDurationString = flightInfoFields.getMaxDaysField().getText();
        List<String> departureAirports = new ArrayList<>();
        for (JTextField departureField : flightInfoFields.getDepartureFields()) {
            String departureAirport = departureField.getText().trim();
            if (!departureAirport.isEmpty()) {
                departureAirports.add(departureAirport);
            }
        }
        //controllo che i campi siano pieni
        if (startDateString.isEmpty() || endDateString.isEmpty() || minDurationString.isEmpty() || maxDurationString.isEmpty() || departureAirports.isEmpty()){
            appendErrorText("Please fill in all fields.",textPane);
            return;
        }

        //controllo tutti gli aereoporti in input siano diversi
        Set<String> uniqueDepartures = new HashSet<>();

        for (JTextField departureField : flightInfoFields.getDepartureFields()) {
            String departureAirport = departureField.getText().trim();
            if (departureAirport.isEmpty()) {
                continue; // Skip empty fields
            }

            if (!uniqueDepartures.add(departureAirport)) {
                JOptionPane.showMessageDialog(parent,
                        "Duplicate departure airport: " + departureAirport,
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        // Controlli date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date today = new Date();
            Date startDate = dateFormat.parse(startDateString);
            Date endDate = dateFormat.parse(endDateString);
            int minDuration = Integer.parseInt(minDurationString);
            int maxDuration = Integer.parseInt(maxDurationString);

            // Controllo che la data di partenza sia maggiore di quella attuale
            if (startDate.before(today)) {
                appendErrorText("Error: Start date must be equal or after the today's date.",textPane);
                return;
            }

            // Controllo che la end date sia successiva alla start date
            if (endDate.before(startDate)) {
                appendErrorText("Error: End date must be equal or after start date.",textPane);
                return;
            }

            // Controllo che min duration sia minore o uguale a max duration
            if (minDuration > maxDuration) {
                appendErrorText("Error: Min duration must be less than or equal to max duration.",textPane);
                return;
            }

            // Calcolo la differenza tra end date e start date in giorni
            long duration = (endDate.getTime() - startDate.getTime()) / (24 * 60 * 60 * 1000);

            // Controllo che la differenza tra end date e start date sia maggiore o uguale a max duration
            if (duration < maxDuration) {
                appendErrorText("Error: End date must be at least " + maxDuration + " days after start date.",textPane);
                return;
            }

        } catch (ParseException | NumberFormatException e) {
            appendErrorText("Error: Invalid date or duration format. Please use yyyy-MM-dd for dates and numerical values for duration.",textPane);
            return;
        }

        // Costruisci l'oggetto ScanQueryRequest
        ScanQueryRequest scanQueryRequest = buildScanQueryRequest(flightInfoFields);

        // Crea un oggetto SwingWorker per eseguire la chiamata API in background
        SwingWorker<ScanQueryResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected ScanQueryResponse doInBackground() throws Exception {
                // Effettua la chiamata API utilizzando la classe ApiGatewayClient
                return apiGatewayClient.sendScanQueryRequest(scanQueryRequest);
            }

            @Override
            protected void done() {
                try {
                    // Ottieni il risultato dell'operazione in background
                    ScanQueryResponse response = get();

                    // Controlla la risposta ottenuta
                    if (response != null) {
                        // La chiamata è andata a buon fine, puoi gestire la risposta qui
                        // Ad esempio, puoi visualizzare i risultati nella console dei risultati
                        ScanOutputView scanOutputView = new ScanOutputView(response.getFlightPairs());
                    } else {
                        // La chiamata ha restituito una risposta nulla, gestisci l'errore
                        appendErrorText("Error: Failed to get valid response from API.", textPane);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Gestisci l'eccezione
                    appendErrorText("Error: An exception occurred during API call.", textPane);
                }
            }
        };

        // Avvia il lavoro in background
        worker.execute();
    }

    public ScanQueryRequest buildScanQueryRequest(FlightInfoFields flightInfoFields) {
        ScanQueryRequest.ScanQueryRequestBuilder builder = ScanQueryRequest.builder();

        // Imposta i valori dal flightInfoFields
        builder.minDays(Integer.valueOf(flightInfoFields.getMinDaysField().getText()));
        builder.maxDays(Integer.valueOf(flightInfoFields.getMaxDaysField().getText()));
        builder.availabilityStart(flightInfoFields.getStartDateField().getText());
        builder.availabilityEnd(flightInfoFields.getEndDateField().getText());
        List<String> departureAirports = new ArrayList<>();
        for (JTextField departureField : flightInfoFields.getDepartureFields()) {
            String departureAirport = departureField.getText().trim();
            if (!departureAirport.isEmpty()) {
                departureAirports.add(departureAirport);
            }
        }
        builder.departureAirports(departureAirports);
        builder.returnToSameAirport(flightInfoFields.getReturnToSameAirportCheckBoxValue());
        List<String> destinationAirports = new ArrayList<>();
        destinationAirports.add("DUB");
        builder.destinationAirports(destinationAirports);
        return builder.build();
    }
}
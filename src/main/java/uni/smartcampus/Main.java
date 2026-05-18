package uni.smartcampus;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;

import uni.smartcampus.model.campus.CampusLayout;
import uni.smartcampus.repo.MeasurementRepository;
import uni.smartcampus.service.MockDataService;
import uni.smartcampus.ui.DashboardFrame;

public class Main {

  private static final String LOGS_PATH   = "generated-data/logs.csv";
  private static final String ALERTS_PATH = "generated-data/alerts.csv";

  public static void main(String[] args) {
    CampusLayout layout = CampusLayout.DEFAULT;
    MockDataService mockDataService = new MockDataService(LOGS_PATH, ALERTS_PATH);
    MeasurementRepository repo = new MeasurementRepository(LOGS_PATH);

    if (!Files.exists(Path.of(LOGS_PATH))) {
      SwingUtilities.invokeLater(() -> SeedingLauncher.seedAndLaunch(layout, mockDataService, repo));
    } else {
      SwingUtilities.invokeLater(() -> new DashboardFrame(layout, repo, mockDataService).setVisible(true));
    }
  }
}

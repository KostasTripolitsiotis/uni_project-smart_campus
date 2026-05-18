package uni.smartcampus;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.util.concurrent.ExecutionException;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import uni.smartcampus.model.campus.CampusLayout;
import uni.smartcampus.repo.MeasurementRepository;
import uni.smartcampus.service.MockDataService;
import uni.smartcampus.ui.DashboardFrame;

class SeedingLauncher {

  private SeedingLauncher() {}

  static void seedAndLaunch(
    CampusLayout layout, MockDataService mockDataService, MeasurementRepository repo
  ) {
    JDialog progress = buildSeedingDialog();
    progress.setVisible(true);

    new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception {
        mockDataService.regenerate(layout);
        return null;
      }

      @Override
      protected void done() {
        progress.dispose();
        if (seedingFailed()) return;
        new DashboardFrame(layout, repo, mockDataService).setVisible(true);
      }

      private boolean seedingFailed() {
        try {
          get();
          return false;
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          showSeedError(ex);
          return true;
        } catch (ExecutionException ex) {
          showSeedError(ex);
          return true;
        }
      }
    }.execute();
  }

  private static void showSeedError(Throwable cause) {
    JOptionPane.showMessageDialog(null,
      "Failed to seed data:\n" + cause.getMessage(),
      "Seed Error", JOptionPane.ERROR_MESSAGE);
  }

  private static JDialog buildSeedingDialog() {
    JDialog d = new JDialog((Frame) null, "Initializing", true);
    d.setModalityType(Dialog.ModalityType.MODELESS);
    JPanel panel = new JPanel(new BorderLayout(10, 12));
    panel.setBorder(new EmptyBorder(20, 28, 20, 28));
    panel.add(new JLabel("Seeding 30 days of mock data, please wait…"), BorderLayout.CENTER);
    JProgressBar bar = new JProgressBar();
    bar.setIndeterminate(true);
    panel.add(bar, BorderLayout.SOUTH);
    d.setContentPane(panel);
    d.pack();
    d.setLocationRelativeTo(null);
    return d;
  }
}

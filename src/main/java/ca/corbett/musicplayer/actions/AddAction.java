package ca.corbett.musicplayer.actions;

import ca.corbett.musicplayer.audio.AudioData;
import ca.corbett.musicplayer.audio.AudioUtil;
import ca.corbett.musicplayer.ui.AudioPanel;
import ca.corbett.musicplayer.ui.MainWindow;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import java.awt.event.ActionEvent;
import java.io.File;

public class AddAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showDialog(MainWindow.getInstance(), "Open") == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                AudioData audioData = AudioUtil.load(file);
                AudioPanel.getInstance().setAudioData(audioData);
            } catch (Exception ex) {
                System.out.println("Something bad happened");
                ex.printStackTrace();
            }
        }
    }
}

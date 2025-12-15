package ca.corbett.musicplayer.audio;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.util.Objects;

/**
 * Represents metadata that we are able to pull from an audio file - for an mp3
 * file, this includes data in the id3 tags (if present), and otherwise,
 * it includes basic information about the file itself.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MusicPlayer 3.1
 */
public class AudioMetadata {

    public static final AudioMetadata NOTHING_PLAYING;

    private String title;
    private String author;
    private String album;
    private String genre;
    private int durationSeconds;
    private File sourceFile;

    static {
        NOTHING_PLAYING = new AudioMetadata();
        NOTHING_PLAYING.title = "(n/a)";
        NOTHING_PLAYING.author = "(n/a)";
        NOTHING_PLAYING.album = "(n/a)";
        NOTHING_PLAYING.genre = "(n/a)";
        NOTHING_PLAYING.durationSeconds = 0;
    }

    private AudioMetadata() {

    }

    /**
     * Parses an AudioMetadata instance from the given audio file.
     * JAudioTagger is used to try to extract id3 tags or similar from the
     * file, if present.
     */
    public static AudioMetadata fromFile(File file) {
        AudioMetadata meta = new AudioMetadata();
        meta.sourceFile = file;
        if (file == null) {
            return meta;
        }

        try {
            AudioFile audioFile = AudioFileIO.read(file);

            // Try to grab the track length, if available:
            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                meta.durationSeconds = header.getTrackLength();
            }

            // Try to grab meta tags, if available:
            Tag tag = audioFile.getTag();
            meta.author = tag.getFirst(FieldKey.ARTIST);
            meta.title = tag.getFirst(FieldKey.TITLE);
            meta.album = tag.getFirst(FieldKey.ALBUM);
            meta.genre = tag.getFirst(FieldKey.GENRE);
        }
        catch (Exception ignored) {
        }

        // If JAudioTagger is unable to extract metadata, then we can try
        // to fill in some guesses based on the file itself.
        if (meta.title == null || meta.title.isBlank()) {
            meta.title = file.getName();
        }
        if (meta.album == null || meta.album.isBlank()) {
            File parent = file.getParentFile();
            meta.album = (parent != null) ? parent.getName() : "(unknown)"; // arbitrary guess
        }

        return meta;
    }

    /**
     * For testing purposes - allows construction of an arbitrary instance of AudioMetadata.
     */
    protected static AudioMetadata fromRawValues(String title, String album, String author, String genre, int duration) {
        AudioMetadata meta = new AudioMetadata();
        meta.title = title;
        meta.album = album;
        meta.author = author;
        meta.genre = genre;
        meta.durationSeconds = duration;
        return meta;
    }

    /**
     * Returns the track duration in a more human-friendly version, instead of seconds.
     * For example, 2461 should return "41:01" indicating minutes:hours.
     */
    public String getDurationFormatted() {
        // special case for small values:
        if (durationSeconds < 60) {
            return "00:" + String.format("%02d", durationSeconds);
        }

        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        while (durationSeconds >= 3600) {
            durationSeconds -= 3600;
            hours++;
        }
        while (durationSeconds >= 60) {
            durationSeconds -= 60;
            minutes++;
        }
        seconds = durationSeconds;

        String hoursStr = "";
        String minutesStr = "";
        String secondsStr = "";
        if (hours > 0) {
            hoursStr = hours + ":";
        }
        if (minutes > 0 || hours > 0) {
            minutesStr = String.format("%02d", minutes) + ":";
        }
        if (seconds > 0 || minutes > 0 || hours > 0) {
            secondsStr = String.format("%02d", seconds);
        }

        return hoursStr + minutesStr + secondsStr;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getAlbum() {
        return album;
    }

    public String getGenre() {
        return genre;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AudioMetadata that)) { return false; }
        return durationSeconds == that.durationSeconds
            && Objects.equals(title, that.title)
            && Objects.equals(author, that.author)
            && Objects.equals(album, that.album)
            && Objects.equals(genre, that.genre)
            && Objects.equals(sourceFile, that.sourceFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, author, album, genre, durationSeconds, sourceFile);
    }
}

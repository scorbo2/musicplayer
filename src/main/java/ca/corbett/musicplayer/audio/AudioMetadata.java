package ca.corbett.musicplayer.audio;

import ca.corbett.extras.StringFormatter;
import ca.corbett.musicplayer.AppConfig;
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

    private String title = "";
    private String author = "";
    private String album = "";
    private String genre = "";
    private int durationSeconds = 0;
    private File sourceFile;
    private int trackNumber = 0;

    static {
        NOTHING_PLAYING = new AudioMetadata();
        NOTHING_PLAYING.title = "(n/a)";
        NOTHING_PLAYING.author = "(n/a)";
        NOTHING_PLAYING.album = "(n/a)";
        NOTHING_PLAYING.genre = "(n/a)";
        NOTHING_PLAYING.durationSeconds = 0;
        NOTHING_PLAYING.trackNumber = 0;
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
            if (tag != null) {
                meta.author = tag.getFirst(FieldKey.ARTIST);
                meta.title = tag.getFirst(FieldKey.TITLE);
                meta.album = tag.getFirst(FieldKey.ALBUM);
                meta.genre = tag.getFirst(FieldKey.GENRE);
                meta.trackNumber = Integer.parseInt(tag.getFirst(FieldKey.TRACK));
            }
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
        if (meta.genre == null) {
            meta.genre = "";
        }
        if (meta.author == null) {
            meta.author = "";
        }

        return meta;
    }

    /**
     * For testing purposes - allows construction of an arbitrary instance of AudioMetadata.
     */
    public static AudioMetadata fromRawValues(String title,
                                              String album,
                                              String author,
                                              String genre,
                                              File sourceFile,
                                              int duration,
                                              int trackNumber) {
        AudioMetadata meta = new AudioMetadata();
        meta.title = title;
        meta.album = album;
        meta.author = author;
        meta.genre = genre;
        meta.durationSeconds = duration;
        meta.sourceFile = sourceFile;
        meta.trackNumber = trackNumber;
        return meta;
    }

    /**
     * Returns a formatted string representing this track's metadata.
     * The format string is provided as an argument.
     */
    public String getFormatted(String formatString) {
        if (formatString == null) {
            formatString = AppConfig.DEFAULT_FORMAT_STRING; // fallback
        }

        return StringFormatter.format(formatString, ch -> {
            final String INVALID_FORMAT_CHAR = "INVALID_FORMAT_CHARACTER";
            String replacement = switch (ch) {
                case 'a' -> getAuthor();
                case 'b' -> getAlbum();
                case 't' -> getTitle();
                case 'n' -> Integer.toString(getTrackNumber());
                case 'g' -> getGenre();
                case 'f' -> getSourceFile() == null ? null : getSourceFile().getName();
                case 'F' -> getSourceFile() == null ? null : getSourceFile().getAbsolutePath();
                case 'd' -> String.valueOf(getDurationSeconds());
                case 'D' -> getDurationFormatted();
                default -> INVALID_FORMAT_CHAR;
            };

            if (INVALID_FORMAT_CHAR.equals(replacement)) {
                return null; // signal to ignore this format char
            }

            // Otherwise, replace null/blank strings with "unknown":
            return replacement != null && !replacement.isBlank() ? replacement : "unknown"; // default placeholder
        });
    }

    /**
     * Returns a formatted string representing this track's metadata.
     * The format string is taken from application configuration.
     */
    public String getFormatted() {
        return getFormatted(AppConfig.getInstance().getPlaylistFormatString());
    }

    /**
     * Returns the track duration in a more human-friendly version, instead of seconds.
     * For example, 2461 should return "41:01" indicating minutes:seconds.
     */
    public String getDurationFormatted() {
        // Negative duration values are impossible, so just flip it:
        if (durationSeconds < 0) {
            durationSeconds = Math.abs(durationSeconds);
        }

        // special case for small values:
        if (durationSeconds < 60) {
            return "00:" + String.format("%02d", durationSeconds);
        }

        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        int remainingSeconds = durationSeconds;
        while (remainingSeconds >= 3600) {
            remainingSeconds -= 3600;
            hours++;
        }
        while (remainingSeconds >= 60) {
            remainingSeconds -= 60;
            minutes++;
        }
        seconds = remainingSeconds;

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

    public int getTrackNumber() {
        return trackNumber;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AudioMetadata that)) { return false; }
        return durationSeconds == that.durationSeconds
            && Objects.equals(title, that.title)
            && Objects.equals(author, that.author)
            && Objects.equals(album, that.album)
            && Objects.equals(genre, that.genre)
            && Objects.equals(trackNumber, that.trackNumber)
            && Objects.equals(sourceFile, that.sourceFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, author, album, genre, durationSeconds, sourceFile, trackNumber);
    }
}

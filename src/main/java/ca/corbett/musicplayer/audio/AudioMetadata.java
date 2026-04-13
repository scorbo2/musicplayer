package ca.corbett.musicplayer.audio;

import ca.corbett.extras.StringFormatter;
import ca.corbett.musicplayer.AppConfig;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Represents metadata that we are able to pull from an audio file - for an mp3
 * file, this includes data in the id3 tags (if present), and otherwise,
 * it includes basic information about the file itself.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since MusicPlayer 3.1
 */
public class AudioMetadata {

    private static final Logger log = Logger.getLogger(AudioMetadata.class.getName());

    /**
     * Callers can listen for changes to our metadata fields.
     * Users can edit this in the TrackInfoDialog. Any UI component
     * that is displaying metadata needs to know when that happens.
     */
    @FunctionalInterface
    public interface ChangeListener {
        void onMetadataChanged(AudioMetadata newMetadata);
    }

    public static final AudioMetadata NOTHING_PLAYING;

    private static final List<ChangeListener> changeListeners = new CopyOnWriteArrayList<>();
    private String title = "";
    private String author = "";
    private String album = "";
    private String genre = "";
    private String lyrics = "";
    private int durationSeconds = 0;
    private File sourceFile;
    private int trackNumber = 0;

    static {
        NOTHING_PLAYING = new AudioMetadata();
        NOTHING_PLAYING.title = "(n/a)";
        NOTHING_PLAYING.author = "(n/a)";
        NOTHING_PLAYING.album = "(n/a)";
        NOTHING_PLAYING.genre = "(n/a)";
        NOTHING_PLAYING.lyrics = "";
        NOTHING_PLAYING.durationSeconds = 0;
        NOTHING_PLAYING.trackNumber = 0;
    }

    private AudioMetadata() {
    }

    /**
     * Registers the given listener to receive notification when ANY AudioMetadata instance changes.
     * Attempting to register a listener that is already registered will be ignored.
     * Attempting to register a null listener will throw an IllegalArgumentException.
     */
    public static void addChangeListener(ChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Change listener cannot be null.");
        }
        // Ignore multiple adds of the same listener:
        if (!changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    /**
     * Stop listening for metadata changes.
     */
    public static void removeChangeListener(ChangeListener listener) {
        if (listener != null) {
            changeListeners.remove(listener);
        }
    }

    /**
     * Reports whether this AudioMetadata came from an mp3 file, based on the source file's extension.
     *
     * @return true if our source file is not null and has a ".mp3" extension (case-insensitive).
     */
    public boolean isMp3() {
        return sourceFile != null && sourceFile.getName().toLowerCase().endsWith(".mp3");
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
                meta.lyrics = tag.getFirst(FieldKey.LYRICS);
                meta.trackNumber = Integer.parseInt(tag.getFirst(FieldKey.TRACK));
            }
        }
        catch (Exception e) {
            log.warning("Unable to read audio metadata from file: " + file.getAbsolutePath() + " - " + e.getMessage());
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
        if (meta.lyrics == null) {
            meta.lyrics = "";
        }

        return meta;
    }

    /**
     * Attempts to save this metadata back to the audio source file, if possible.
     * Currently, this is only supported for mp3 files - attempt to save metadata
     * to any other file type will throw an IOException.
     *
     * @throws IOException If something goes wrong.
     */
    public void saveMetadata() throws IOException {
        if (!isMp3()) {
            throw new IOException("Unable to save metadata - source file is not an mp3 file.");
        }
        try {
            AudioFile audioFile = AudioFileIO.read(sourceFile);
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            tag.setField(FieldKey.ARTIST, getAuthor());
            tag.setField(FieldKey.TITLE, getTitle());
            tag.setField(FieldKey.ALBUM, getAlbum());
            tag.setField(FieldKey.GENRE, getGenre());
            tag.setField(FieldKey.TRACK, Integer.toString(getTrackNumber()));
            tag.setField(FieldKey.LYRICS, getLyrics());
            audioFile.commit();
            fireChangeEvent(); // Only send a change event after a successful save, not when our fields are updated.
        }
        catch (Exception e) {
            // The audio code can throw many different types of exceptions.
            // We'll catch them and wrap them in an IOException for simplicity.
            throw new IOException("Unable to save audio metadata to "
                                      + sourceFile.getAbsolutePath() + ": " + e.getMessage(), e);
        }
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
        return fromRawValues(title, album, author, genre, sourceFile, duration, trackNumber, "");
    }

    public static AudioMetadata fromRawValues(String title,
                                              String album,
                                              String author,
                                              String genre,
                                              File sourceFile,
                                              int duration,
                                              int trackNumber,
                                              String lyrics) {
        AudioMetadata meta = new AudioMetadata();
        meta.title = title;
        meta.album = album;
        meta.author = author;
        meta.genre = genre;
        meta.durationSeconds = duration;
        meta.sourceFile = sourceFile;
        meta.trackNumber = trackNumber;
        meta.lyrics = lyrics;
        return meta;
    }

    /**
     * Returns a formatted string representing this track's metadata.
     * The format string is provided as an argument.
     * Note: lyrics are not available as a format string option (too long to display).
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

    public String getLyrics() {
        return lyrics;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public void setAuthor(String author) {
        this.author = author == null ? "" : author;
    }

    public void setAlbum(String album) {
        this.album = album == null ? "" : album;
    }

    public void setGenre(String genre) {
        this.genre = genre == null ? "" : genre;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = Math.max(trackNumber, 0); // negative track numbers don't make sense
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

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics == null ? "" : lyrics;
    }

    /**
     * This is a weaker version of equals() that only checks to see if two
     * AudioMetadata instances came from the same source file. This is useful for determining
     * whether two AudioMetadata instances are referring to the same track, even if their metadata fields
     * might be different (for example, if the user edited the metadata in one of them, but not the other).
     * If anything is null, returns false.
     *
     * @param other Any AudioMetadata instance to compare to this one.
     * @return true if the other instance is not null, has a non-null source file, and it matches ours.
     */
    public boolean hasSameSourceFile(AudioMetadata other) {
        if (other == null) {
            return false;
        }
        if (this.sourceFile == null || other.sourceFile == null) {
            return false;
        }
        return this.sourceFile.equals(other.sourceFile);
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

    private void fireChangeEvent() {
        for (ChangeListener listener : changeListeners) {
            listener.onMetadataChanged(this);
        }
    }
}

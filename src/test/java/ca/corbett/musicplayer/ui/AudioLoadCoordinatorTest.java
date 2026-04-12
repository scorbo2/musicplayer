package ca.corbett.musicplayer.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioLoadCoordinatorTest {

    private AudioLoadCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = AudioLoadCoordinator.getInstance();
        // Cancel any pending requests to start each test from a clean, known state.
        coordinator.cancelPendingRequests();
    }

    // -----------------------------------------------------------------------
    // requestLoad: request ID management
    // -----------------------------------------------------------------------

    @Test
    void requestLoad_withNullFile_shouldReturnCurrentIdUnchanged() {
        // GIVEN an initial latest request id:
        long before = coordinator.getLatestRequestId();

        // WHEN we try to load a null file:
        long returned = coordinator.requestLoad(null);

        // THEN the returned id and the latest id should both be unchanged:
        assertEquals(before, returned);
        assertEquals(before, coordinator.getLatestRequestId());
    }

    @Test
    void requestLoad_withValidFile_shouldReturnIncrementingId() {
        // GIVEN the current latest id:
        long before = coordinator.getLatestRequestId();

        // WHEN we request a load (file need not exist for ID tracking):
        long id1 = coordinator.requestLoad(new File("/nonexistent/track1.mp3"));
        coordinator.cancelPendingRequests(); // prevent background processing

        // THEN the returned id should be strictly greater than the previous one:
        assertTrue(id1 > before, "requestLoad should return an id greater than the previous latest id");
        // AND the latest request id should match the returned id before cancellation bumps it.
    }

    @Test
    void requestLoad_withSequentialCalls_shouldReturnStrictlyIncreasingIds() {
        // WHEN we issue multiple load requests:
        long id1 = coordinator.requestLoad(new File("/nonexistent/a.mp3"));
        long id2 = coordinator.requestLoad(new File("/nonexistent/b.mp3"));
        long id3 = coordinator.requestLoad(new File("/nonexistent/c.mp3"));
        coordinator.cancelPendingRequests(); // prevent background processing

        // THEN each successive id should be strictly larger:
        assertTrue(id2 > id1, "Second request id should be greater than first");
        assertTrue(id3 > id2, "Third request id should be greater than second");
    }

    // -----------------------------------------------------------------------
    // Only the latest request applies / stale requests are ignored
    // -----------------------------------------------------------------------

    @Test
    void requestLoad_withMultipleRapidRequests_onlyLatestIsCurrent() {
        // GIVEN three rapid load requests:
        long id1 = coordinator.requestLoad(new File("/nonexistent/a.mp3"));
        long id2 = coordinator.requestLoad(new File("/nonexistent/b.mp3"));
        long id3 = coordinator.requestLoad(new File("/nonexistent/c.mp3"));
        coordinator.cancelPendingRequests(); // prevent background processing

        // THEN only the last id issued before cancellation was the latest at that point.
        // id1 and id2 are stale relative to id3:
        assertFalse(coordinator.isCurrentRequest(id1), "id1 should be stale after id2 and id3 were issued");
        assertFalse(coordinator.isCurrentRequest(id2), "id2 should be stale after id3 was issued");
        // After cancelPendingRequests() the counter is bumped so id3 is also stale:
        assertFalse(coordinator.isCurrentRequest(id3), "id3 should be stale after cancelPendingRequests() bumps the counter");
    }

    @Test
    void isCurrentRequest_withLatestId_shouldReturnTrue() {
        // WHEN we queue a single request:
        long id = coordinator.requestLoad(new File("/nonexistent/track.mp3"));

        // THEN isCurrentRequest should return true for that id:
        assertTrue(coordinator.isCurrentRequest(id), "The freshly issued id should be current");

        coordinator.cancelPendingRequests(); // cleanup
    }

    @Test
    void isCurrentRequest_withOlderIdAfterNewerRequest_shouldReturnFalse() {
        // GIVEN a first request:
        long staleId = coordinator.requestLoad(new File("/nonexistent/first.mp3"));

        // WHEN a second request supersedes it:
        long currentId = coordinator.requestLoad(new File("/nonexistent/second.mp3"));
        coordinator.cancelPendingRequests(); // prevent background processing

        // THEN only the most recent non-cancelled id was current just before cancellation.
        // After cancel both are stale; the important thing is staleId != currentId:
        assertNotEquals(staleId, currentId, "Successive requests should produce different ids");
        assertFalse(coordinator.isCurrentRequest(staleId), "staleId should not be current after it was superseded");
    }

    @Test
    void isCurrentRequest_withZeroId_shouldAlwaysReturnFalse() {
        // THEN requestId == 0 is never a valid current request:
        assertFalse(coordinator.isCurrentRequest(0), "id 0 should never be a current request");
    }

    @Test
    void isCurrentRequest_withNegativeId_shouldAlwaysReturnFalse() {
        assertFalse(coordinator.isCurrentRequest(-1), "Negative ids should never be current");
    }

    // -----------------------------------------------------------------------
    // cancelPendingRequests: prevents stale results from applying
    // -----------------------------------------------------------------------

    @Test
    void cancelPendingRequests_shouldInvalidateCurrentRequest() {
        // GIVEN an active load request:
        long id = coordinator.requestLoad(new File("/nonexistent/track.mp3"));
        assertTrue(coordinator.isCurrentRequest(id), "id should be current before cancellation");

        // WHEN we cancel:
        coordinator.cancelPendingRequests();

        // THEN the previously current id should now be stale:
        assertFalse(coordinator.isCurrentRequest(id), "id should be stale after cancelPendingRequests()");
    }

    @Test
    void cancelPendingRequests_withNoPendingRequest_shouldBeIdempotent() {
        // WHEN cancel is called with no active requests (already clean from setUp):
        long idBefore = coordinator.getLatestRequestId();
        coordinator.cancelPendingRequests();
        long idAfter = coordinator.getLatestRequestId();

        // THEN the coordinator should still be in a valid (incremented) state:
        assertTrue(idAfter > idBefore, "cancelPendingRequests should increment the id even with no pending request");
    }

    // -----------------------------------------------------------------------
    // Waveform refresh throttling
    // -----------------------------------------------------------------------

    @Test
    void isWaveformRefreshThrottled_initialState_shouldNotBeThrottled() {
        // GIVEN the coordinator is freshly reset (cancelPendingRequests resets waveform state):
        // THEN a non-forced refresh should NOT be throttled (no recent refresh, no queued refresh):
        assertFalse(coordinator.isWaveformRefreshThrottled(false),
                    "Waveform refresh should not be throttled in the initial state");
    }

    @Test
    void isWaveformRefreshThrottled_whenRefreshQueuedFlag_shouldBeThrottled() {
        // GIVEN the queued flag is set:
        coordinator.setWaveformRefreshQueuedForTesting(true);

        // THEN a non-forced refresh should be throttled:
        assertTrue(coordinator.isWaveformRefreshThrottled(false),
                   "Waveform refresh should be throttled when a refresh is already queued");

        // Cleanup
        coordinator.cancelPendingRequests();
    }

    @Test
    void isWaveformRefreshThrottled_whenWithinThrottleInterval_shouldBeThrottled() {
        // GIVEN the last UI refresh happened just now:
        coordinator.setLastWaveformRefreshMillisForTesting(System.currentTimeMillis());

        // THEN a non-forced refresh should be throttled (within the minimum interval):
        assertTrue(coordinator.isWaveformRefreshThrottled(false),
                   "Waveform refresh should be throttled when the last refresh was very recent");

        // Cleanup
        coordinator.cancelPendingRequests();
    }

    @Test
    void isWaveformRefreshThrottled_whenIntervalElapsed_shouldNotBeThrottled() {
        // GIVEN the last refresh happened long ago (simulate with timestamp 0):
        coordinator.setLastWaveformRefreshMillisForTesting(0);
        coordinator.setWaveformRefreshQueuedForTesting(false);

        // THEN a non-forced refresh should not be throttled:
        assertFalse(coordinator.isWaveformRefreshThrottled(false),
                    "Waveform refresh should not be throttled after the throttle interval has elapsed");
    }

    @Test
    void isWaveformRefreshThrottled_withForceRefresh_shouldNeverBeThrottled() {
        // GIVEN the most aggressive throttle conditions (very recent refresh + queued):
        coordinator.setLastWaveformRefreshMillisForTesting(System.currentTimeMillis());
        coordinator.setWaveformRefreshQueuedForTesting(true);

        // WHEN forceRefresh is true:
        // THEN the refresh should NOT be throttled regardless:
        assertFalse(coordinator.isWaveformRefreshThrottled(true),
                    "A forced waveform refresh should never be throttled");

        // Cleanup
        coordinator.cancelPendingRequests();
    }
}

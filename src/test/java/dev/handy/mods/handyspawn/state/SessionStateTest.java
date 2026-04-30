package dev.handy.mods.handyspawn.state;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SessionStateTest {

    @Test
    void initiallyOutOfSessionAndUnsynced() {
        SessionState s = new SessionState();
        assertFalse(s.inSession());
        assertFalse(s.isServerSynced());
    }

    @Test
    void handshakeAfterJoinMarksServerSynced() {
        SessionState s = new SessionState();
        s.onJoin();
        s.onHandshakeReceived();
        assertTrue(s.isServerSynced());
    }

    @Test
    void handshakeWithoutJoinIsIgnored() {
        SessionState s = new SessionState();
        s.onHandshakeReceived();
        assertFalse(s.isServerSynced());
    }

    @Test
    void fallbackFiresOnceAfterThreshold() {
        SessionState s = new SessionState();
        s.onJoin();
        AtomicInteger count = new AtomicInteger();
        AtomicReference<String> lastMsg = new AtomicReference<>();
        for (int i = 0; i < SessionState.FALLBACK_AFTER_TICKS + 10; i++) {
            s.onTick(msg -> {
                count.incrementAndGet();
                lastMsg.set(msg);
            });
        }
        assertEquals(1, count.get());
        assertNotNull(lastMsg.get());
    }

    @Test
    void fallbackDoesNotFireIfHandshakeArrivesInTime() {
        SessionState s = new SessionState();
        s.onJoin();
        for (int i = 0; i < 30; i++) s.onTick(msg -> fail("should not fire"));
        s.onHandshakeReceived();
        for (int i = 0; i < 100; i++) s.onTick(msg -> fail("should not fire"));
    }

    @Test
    void disconnectResetsEverything() {
        SessionState s = new SessionState();
        s.onJoin();
        s.onHandshakeReceived();
        s.onDisconnect();
        assertFalse(s.inSession());
        assertFalse(s.isServerSynced());
    }
}

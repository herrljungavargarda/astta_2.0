package se.herrljunga.astta;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import se.herrljunga.astta.filehandler.StorageHandler;
import se.herrljunga.astta.filehandler.BlobStorageHandler;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FetchSaveTest {
    @Disabled
    @Test
    public void testFetch() {
        StorageHandler fetchSave = new BlobStorageHandler("https://hvprdrg20audio.blob.core.windows.net",
                "?sv=2022-11-02&ss=bfqt&srt=sco&sp=rwdlacupiytfx&se=2024-02-06T19:59:02Z&st=2024-02-06T11:59:02Z&spr=https&sig=81nXrMr95xpk98NZ8Wkvz7TTz0XaCPvgRNqMPRNxyJ0%3D",
                "test");
        List<byte[]> result = fetchSave.fetchByte();
        assertThat(result).isNotEmpty();
    }
}
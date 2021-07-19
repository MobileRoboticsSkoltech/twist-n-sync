import com.googleresearch.capturesync.wrapperJNI;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class wrapperJNITests {

    @Test
    public void transferByteToJNIIsCorrect() throws IOException {
        String filePath = "testFile1.csv";
        byte[] currentByteArray = Files.readAllBytes(Paths.get(filePath));

        assertEquals(1.2107227, wrapperJNI.sendData(currentByteArray), 0.1);
    }
}
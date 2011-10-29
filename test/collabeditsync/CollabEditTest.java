package collabeditsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.editor.Document;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CollabEditTest {

    private CollabEdit collabEdit = new CollabEdit();

    @Before
    public void setUp() throws Exception {
        collabEdit.resource = mock(CollabEditResource.class);
    }

    @Test
    public void modificationContainsBothInsertAndDeleteCommandsWithOffsets() throws Exception {
        String data = "{\"ops\":[[7,1],[9,\"zxxxx\"],[8,\"b\"],[7,20]],\"cuid\":745713,\"parent_hash\":\"b6c963fa53e083f41389c8478b88057c\",\"result_hash\":\"e001a5db8a6f1f16b5c457779c2156a0\"}";
        JSONObject jsonData = (JSONObject) JSONSerializer.toJSON(data);
        when(collabEdit.resource.waitForUpdate(false)).thenReturn(jsonData);

        Command command = collabEdit.waitForModificationCommand();
        assertEquals(1, command.delete.startOffset);
        assertEquals(6, command.delete.endOffset);
        assertEquals(1, command.insert.offset);
        assertEquals("b", command.insert.text);
    }

    @Test
    public void modificationContainsOnlyInsertWithOffsets() throws Exception {
        String data = "{\"ops\":[[7,1],[8,\"b\"],[7,20]],\"cuid\":745713,\"parent_hash\":\"b6c963fa53e083f41389c8478b88057c\",\"result_hash\":\"e001a5db8a6f1f16b5c457779c2156a0\"}";
        JSONObject jsonData = (JSONObject) JSONSerializer.toJSON(data);
        when(collabEdit.resource.waitForUpdate(false)).thenReturn(jsonData);

        Command command = collabEdit.waitForModificationCommand();
        assertNull(command.delete);
        assertEquals(1, command.insert.offset);
        assertEquals("b", command.insert.text);
    }


    @Test
    public void modificationContainsOnlyDeleteWithOffsets() throws Exception {
        String data = "{\"ops\":[[7,1],[9,\"zxxxx\"],[7,20]],\"cuid\":745713,\"parent_hash\":\"b6c963fa53e083f41389c8478b88057c\",\"result_hash\":\"e001a5db8a6f1f16b5c457779c2156a0\"}";
        JSONObject jsonData = (JSONObject) JSONSerializer.toJSON(data);
        when(collabEdit.resource.waitForUpdate(false)).thenReturn(jsonData);

        Command command = collabEdit.waitForModificationCommand();
        assertEquals(1, command.delete.startOffset);
        assertEquals(6, command.delete.endOffset);
        assertNull(command.insert);
    }

    @Test
    public void modificationContainsOnlyDeleteWithEndOffset() throws Exception {
        String data = "{\"ops\":[[9,\"zxxxx\"],[7,20]],\"cuid\":745713,\"parent_hash\":\"b6c963fa53e083f41389c8478b88057c\",\"result_hash\":\"e001a5db8a6f1f16b5c457779c2156a0\"}";
        JSONObject jsonData = (JSONObject) JSONSerializer.toJSON(data);
        when(collabEdit.resource.waitForUpdate(false)).thenReturn(jsonData);

        Command command = collabEdit.waitForModificationCommand();
        assertEquals(0, command.delete.startOffset);
        assertEquals(5, command.delete.endOffset);
    }



}

package collabeditsync;

import com.intellij.openapi.editor.Document;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static collabeditsync.Command.Operation.DELETE;
import static collabeditsync.Command.Operation.INSERT;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class CollabEditTest {

    private CollabEdit collabEdit = new CollabEdit();

    @Before
    public void setUp() throws Exception {
        collabEdit.resource = mock(CollabEditResource.class);
    }

    @Test
    public void modificationContainsBothInsertAndDeleteCommandsWithOffsets() throws Exception {
        String data = "{\"op\":{\"ops\":[[7,1],[9,\"zxxxx\"],[8,\"b\"],[7,20]],\"cuid\":745713,\"parent_hash\":\"b6c963fa53e083f41389c8478b88057c\",\"result_hash\":\"e001a5db8a6f1f16b5c457779c2156a0\"}}";
        mockWaitForUpdate(data);

        Command command = collabEdit.waitForModificationCommands();
        assertEquals(DELETE, command.diffs.get(0).operation);
        assertEquals(1, command.diffs.get(0).startOffset);
        assertEquals(6, command.diffs.get(0).endOffset);
        assertEquals(INSERT, command.diffs.get(1).operation);
        assertEquals(1, command.diffs.get(1).startOffset);
        assertEquals("b", command.diffs.get(1).text);
    }

    @Test
    public void modificationContainsOnlyInsertWithOffsets() throws Exception {
        String data = "{\"op\":{\"ops\":[[7,1],[8,\"b\"],[7,20]],\"cuid\":745713,\"parent_hash\":\"b6c963fa53e083f41389c8478b88057c\",\"result_hash\":\"e001a5db8a6f1f16b5c457779c2156a0\"}}";
        mockWaitForUpdate(data);

        Command command = collabEdit.waitForModificationCommands();
        assertEquals(INSERT, command.diffs.get(0).operation);
        assertEquals(1, command.diffs.get(0).startOffset);
        assertEquals("b", command.diffs.get(0).text);
    }


    @Test
    public void modificationContainsOnlyDeleteWithOffsets() throws Exception {
        mockWaitForUpdate("{\"op\":{\"ops\":[[7,1],[9,\"zxxxx\"],[7,20]],\"cuid\":745713,\"parent_hash\":\"b6c963fa53e083f41389c8478b88057c\",\"result_hash\":\"e001a5db8a6f1f16b5c457779c2156a0\"}}");

        Command command = collabEdit.waitForModificationCommands();
        assertEquals(DELETE, command.diffs.get(0).operation);
        assertEquals(1, command.diffs.get(0).startOffset);
        assertEquals(6, command.diffs.get(0).endOffset);
    }

    @Test
    public void modificationContainsOnlyDeleteWithEndOffset() throws Exception {
        mockWaitForUpdate("{\"op\":{\"ops\":[[9,\"zxxxx\"],[7,20]],\"cuid\":745713,\"parent_hash\":\"b6c963fa53e083f41389c8478b88057c\",\"result_hash\":\"e001a5db8a6f1f16b5c457779c2156a0\"}}");

        Command command = collabEdit.waitForModificationCommands();
        assertEquals(DELETE, command.diffs.get(0).operation);
        assertEquals(0, command.diffs.get(0).startOffset);
        assertEquals(5, command.diffs.get(0).endOffset);
    }


    @Test
    @Ignore
    public void multipleDiffs() throws Exception {
        String before = "asdasdfada\nasdsasaadsdsfasdfassfadsf\nasdsadasdf";

        String data = "{\"op\":{\"ops\":[[7,10],[8,\"sdf\"],[7,15],[8,\"sfa\"],[7,22]],\"cuid\":746859,\"parent_hash\":\"6673e124dbdaf19775d4ee41471ed982\",\"result_hash\":\"dc6eb65a7f3378189d3c6357ff7aee66\"}}";

        String after =  "asdasdfadasdf\nasdsasaadsdsfasfasdfassfadsf\nasdsadasdf";

        mockWaitForUpdate(data);

        Command command = collabEdit.waitForModificationCommands();
        System.out.println(command);

        assertEquals(DELETE, command.diffs.get(0).operation);
        assertEquals(0, command.diffs.get(0).startOffset);
        assertEquals(5, command.diffs.get(0).endOffset);
    }

    private void mockWaitForUpdate(String data) throws IOException, UnsuccessfulResponseException {
        JSONObject jsonData = (JSONObject) JSONSerializer.toJSON(data);
        when(collabEdit.resource.waitForUpdate(false)).thenReturn(jsonData);
    }

    @Test
    public void waitForDataUntilOpsReceived() throws Exception {
        String firstData = "{\"lang\":\"none\",\"messages\":[{\"message_text\":\"renamed document to dd\",\"nickname\":\"Jevgeni\",\"type\":5}],\"name\":\"dd\",\"user_list\":[\"bob\"]}";
        String secondData = "{\"op\":{\"ops\":[[9,\"zxxxx\"],[7,20]],\"cuid\":745713,\"parent_hash\":\"6673e124dbdaf19775d4ee41471ed982\",\"result_hash\":\"dc6eb65a7f3378189d3c6357ff7aee66\"}}";
        JSONObject firstJsonData = (JSONObject) JSONSerializer.toJSON(firstData);
        JSONObject secondJsonData = (JSONObject) JSONSerializer.toJSON(secondData);
        when(collabEdit.resource.waitForUpdate(false)).thenReturn(firstJsonData).thenReturn(secondJsonData);

        Command command = collabEdit.waitForModificationCommands();
        assertEquals(DELETE, command.diffs.get(0).operation);
    }

    @Test
    public void applyModificationToDocument() throws Exception {
        Command command = new Command(123, "a", "b", asList(new Command.Diff(DELETE, 3, "qwerty"), new Command.Diff(INSERT, 3, "test")));
        Document document = mock(Document.class);
        command.apply(document);

        verify(document).deleteString(3, 9);
        verify(document).insertString(3, "test");
    }

    @Test
    public void sendUpdatesOnlyForDifferences() throws Exception {
        collabEdit.update("x", "x");
        verifyZeroInteractions(collabEdit.resource);
    }

    @Test
    public void requestFullSyncIfProblemsWithUpdate() throws Exception {
        collabEdit = spy(collabEdit);

         doThrow(new UnsuccessfulResponseException()).doNothing()
                 .when(collabEdit).diffUpdate(anyString(), anyString());

        JSONObject jsonObject = new JSONObject();
        jsonObject.accumulate("full_text", "xxx");
        when(collabEdit.resource.waitForUpdate(true)).thenReturn(jsonObject);

        collabEdit.update("old-text", "new-text");
        verify(collabEdit).fullUpdate("new-text");
    }

    @Test
    public void checkIfCommandIsMyOwn() throws Exception {
        Command command = new Command(123, null, null, null);
        collabEdit.setCuid(123);
        assertTrue(collabEdit.isMyOwnCommand(command));
        collabEdit.setCuid(567);
        assertFalse(collabEdit.isMyOwnCommand(command));
    }

    @Test
    public void waitForNotOwnModificationCommand() throws Exception {
        collabEdit = spy(collabEdit);
        collabEdit.setCuid(123);
        Command c1 = new Command(123, null, null, null);
        Command c2 = new Command(567, null, null, null);
        doReturn(c1).doReturn(c2).when(collabEdit).waitForModificationCommands();

        assertEquals(c2, collabEdit.waitForExternalModificationCommand());
    }

    @Test
    public void insertDeleteDiff() throws Exception {
        collabEdit.diffUpdate("ABC", "AxC");
        verify(collabEdit.resource).sendUpdate(1, "B", "x", "AxC");
    }

    @Test
    public void insertDiffAtFront() throws Exception {
        collabEdit.diffUpdate("ABC", "aABC");
        verify(collabEdit.resource).sendUpdate(0, "", "a", "aABC");
        verifyNoMoreInteractions(collabEdit.resource);
    }

    @Test
    public void insertDiffAtEnd() throws Exception {
        collabEdit.diffUpdate("ABC", "ABCd");
        verify(collabEdit.resource).sendUpdate(3, "", "d", "ABCd");
        verifyNoMoreInteractions(collabEdit.resource);
    }

    @Test
    public void insertDiffInTheMiddle() throws Exception {
        collabEdit.diffUpdate("ABC", "ABxC");
        verify(collabEdit.resource).sendUpdate(2, "", "x", "ABxC");
        verifyNoMoreInteractions(collabEdit.resource);
    }

    @Test
    public void deleteDiffAtFront() throws Exception {
        collabEdit.diffUpdate("xABC", "ABC");
        verify(collabEdit.resource).sendUpdate(0, "x", "", "ABC");
        verifyNoMoreInteractions(collabEdit.resource);
    }

    @Test
    public void deleteDiffAtEnd() throws Exception {
        collabEdit.diffUpdate("ABCx", "ABC");
        verify(collabEdit.resource).sendUpdate(3, "x", "", "ABC");
        verifyNoMoreInteractions(collabEdit.resource);
    }

    @Test
    public void deleteDiffInTheMiddle() throws Exception {
        collabEdit.diffUpdate("ABxC", "ABC");
        verify(collabEdit.resource).sendUpdate(2, "x", "", "ABC");
        verifyNoMoreInteractions(collabEdit.resource);
    }

        @Test
    public void insertDeleteDiffMultipleTimes() throws Exception {
        collabEdit.diffUpdate("ABCDEF", "AyCDxxF");
        verify(collabEdit.resource).sendUpdate(1, "B", "y", "AyCDEF");
        verify(collabEdit.resource).sendUpdate(4, "E", "xx", "AyCDxxF");
        verifyNoMoreInteractions(collabEdit.resource);
    }


    // TODO: remove if document is empty?
//
//   Received: {"op":{"ops":[[9,"p"]],"cuid":745865,"parent_hash":"83878c91171338902e0fe0fb97a8c47a","result_hash":"d41d8cd98f00b204e9800998ecf8427e"}}
//[ 906781]  ERROR - pplication.impl.LaterInvocator - Wrong endOffset: 1; documentLength: 0
//java.lang.IndexOutOfBoundsException: Wrong endOffset: 1; documentLength: 0
//	at com.intellij.openapi.editor.impl.DocumentImpl.b(DocumentImpl.java:410)
//	at com.intellij.openapi.editor.impl.DocumentImpl.deleteString(DocumentImpl.java:345)

}

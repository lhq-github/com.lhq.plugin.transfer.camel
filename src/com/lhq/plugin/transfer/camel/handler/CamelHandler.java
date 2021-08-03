package com.lhq.plugin.transfer.camel.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

public class CamelHandler extends AbstractHandler {
	
	@Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart iep = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ITextEditor ite = null;
		TextSelection selection = null;
		IDocument document = null;
		ISelectionProvider selectionProvider = null;
		if ((iep instanceof ITextEditor)) {
			ite = (ITextEditor) iep;
			selectionProvider = ite.getSelectionProvider();
			selection = (TextSelection) selectionProvider.getSelection();
			document = ite.getDocumentProvider().getDocument(ite.getEditorInput());
		} else if ((iep instanceof MultiPageEditorPart)) {
			MultiPageEditorPart part = (MultiPageEditorPart) iep;
			Object sp = part.getSelectedPage();
			if (!(sp instanceof StructuredTextEditor)) {
				return null;
			}
			ite = (StructuredTextEditor) sp;
			selectionProvider = ite.getSelectionProvider();
			selection = (TextSelection) selectionProvider.getSelection();
			document = ite.getDocumentProvider().getDocument(ite.getEditorInput());
		} else {
			return null;
		}

		if ((selection == null) || (document == null) || (selectionProvider == null)) {
			return null;
		}

		String title = iep.getTitle();
        TextProcessResult result = process(document.get(), selection.getOffset(), selection.getLength(), (title != null) && (title.endsWith(".java")));
		if (result == null) {
			return null;
		}
		try {
			document.replace(result.replaceOffset, result.replaceLength, result.replaceText);
		} catch (BadLocationException e) {
			return null;
		}
		ite.selectAndReveal(result.newSelectionOffset, result.newSelectionLength);
		return null;
	}

	protected TextProcessResult process(String text, int selectionOffset, int selectionLength, boolean isJavaFile) {
		if ((text == null) || (selectionOffset < 0) || (selectionLength <= 2)) {
			return null;
		}

		String selected = text.substring(selectionOffset, selectionOffset + selectionLength);
		if (selected.indexOf('_') != -1) {
            selected = selected.toLowerCase();
			selected = selected.replaceAll("_([A-Z])", "$1");
			int i = -32;
			for (char c = 'a'; c <= 'z'; c = (char) (c + '\001'))
				selected = selected.replaceAll("_" + c, String.valueOf((char) (c + i)));
		} else {
			selected = selected.replaceAll("(?<![A-Z])([A-Z])", "_$1");
			selected = selected.toLowerCase();
			if (selected.charAt(0) == '_') {
				selected = selected.substring(1);
			}
		}

		TextProcessResult result = new TextProcessResult();
		result.replaceText = selected;
		result.newSelectionOffset = selectionOffset;
		result.newSelectionLength = selected.length();
		result.replaceOffset = selectionOffset;
		result.replaceLength = selectionLength;

		return result;
	}

	protected static final class TextProcessResult {
		int newSelectionOffset;
		int newSelectionLength;
		int replaceOffset;
		int replaceLength;
		String replaceText;
	}
}
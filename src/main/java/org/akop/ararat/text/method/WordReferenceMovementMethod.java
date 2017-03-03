// Copyright (c) 2014-2016 Akop Karapetyan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package org.akop.ararat.text.method;

import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.akop.ararat.core.Crossword;
import org.akop.ararat.util.ReferenceScanner;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is a modified version of LinkMovementMethod that handles
 * URL's of form wordRef://direction/number by invoking listener
 * OnWordReferenceSelectedListener (if specified)
 */
public class WordReferenceMovementMethod
		extends ScrollingMovementMethod
{
	private static Pattern EXTRACT_REF
			= Pattern.compile("^(\\w+)://(\\d+)/(\\d+)$");

	private static final String PROTOCOL_REF      = "ref";
	private static final String PROTOCOL_CITATION = "cite";

	public interface OnWordReferenceSelectedListener
	{
		void onWordReferenceSelected(int direction, int number);
		void onCitationRequested(int direction, int number);
	}

	private OnWordReferenceSelectedListener mWordRefListener;

	private static final int CLICK = 1;
	private static final int UP = 2;
	private static final int DOWN = 3;

	public void setOnWordReferenceSelectedListener(OnWordReferenceSelectedListener listener)
	{
		mWordRefListener = listener;
	}

	@Override
	public boolean canSelectArbitrarily()
	{
		return true;
	}

	@Override
	protected boolean handleMovementKey(TextView widget, Spannable buffer, int keyCode,
										int movementMetaState, KeyEvent event)
	{
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_ENTER:
			if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) {
				if (event.getAction() == KeyEvent.ACTION_DOWN &&
						event.getRepeatCount() == 0 && action(CLICK, widget, buffer)) {
					return true;
				}
			}
			break;
		}
		return super.handleMovementKey(widget, buffer, keyCode, movementMetaState, event);
	}

	@Override
	protected boolean up(TextView widget, Spannable buffer)
	{
		if (action(UP, widget, buffer)) {
			return true;
		}

		return super.up(widget, buffer);
	}

	@Override
	protected boolean down(TextView widget, Spannable buffer)
	{
		if (action(DOWN, widget, buffer)) {
			return true;
		}

		return super.down(widget, buffer);
	}

	@Override
	protected boolean left(TextView widget, Spannable buffer)
	{
		if (action(UP, widget, buffer)) {
			return true;
		}

		return super.left(widget, buffer);
	}

	@Override
	protected boolean right(TextView widget, Spannable buffer)
	{
		if (action(DOWN, widget, buffer)) {
			return true;
		}

		return super.right(widget, buffer);
	}

	private boolean action(int what, TextView widget, Spannable buffer)
	{
		Layout layout = widget.getLayout();

		int padding = widget.getTotalPaddingTop() +
				widget.getTotalPaddingBottom();
		int areatop = widget.getScrollY();
		int areabot = areatop + widget.getHeight() - padding;

		int linetop = layout.getLineForVertical(areatop);
		int linebot = layout.getLineForVertical(areabot);

		int first = layout.getLineStart(linetop);
		int last = layout.getLineEnd(linebot);

		ClickableSpan[] candidates = buffer.getSpans(first, last, ClickableSpan.class);

		int a = Selection.getSelectionStart(buffer);
		int b = Selection.getSelectionEnd(buffer);

		int selStart = Math.min(a, b);
		int selEnd = Math.max(a, b);

		if (selStart < 0) {
			if (buffer.getSpanStart(FROM_BELOW) >= 0) {
				selStart = selEnd = buffer.length();
			}
		}

		if (selStart > last)
			selStart = selEnd = Integer.MAX_VALUE;
		if (selEnd < first)
			selStart = selEnd = -1;

		switch (what) {
		case CLICK:
			if (selStart == selEnd) {
				return false;
			}

			ClickableSpan[] link = buffer.getSpans(selStart, selEnd, ClickableSpan.class);

			if (link.length != 1)
				return false;

			handleClick(widget, link[0]);
			break;

		case UP:
			int beststart, bestend;

			beststart = -1;
			bestend = -1;

			for (int i = 0; i < candidates.length; i++) {
				int end = buffer.getSpanEnd(candidates[i]);

				if (end < selEnd || selStart == selEnd) {
					if (end > bestend) {
						beststart = buffer.getSpanStart(candidates[i]);
						bestend = end;
					}
				}
			}

			if (beststart >= 0) {
				Selection.setSelection(buffer, bestend, beststart);
				return true;
			}

			break;

		case DOWN:
			beststart = Integer.MAX_VALUE;
			bestend = Integer.MAX_VALUE;

			for (int i = 0; i < candidates.length; i++) {
				int start = buffer.getSpanStart(candidates[i]);

				if (start > selStart || selStart == selEnd) {
					if (start < beststart) {
						beststart = start;
						bestend = buffer.getSpanEnd(candidates[i]);
					}
				}
			}

			if (bestend < Integer.MAX_VALUE) {
				Selection.setSelection(buffer, beststart, bestend);
				return true;
			}

			break;
		}

		return false;
	}

	@Override
	public boolean onTouchEvent(TextView widget, Spannable buffer,
								MotionEvent event)
	{
		int action = event.getAction();

		if (action == MotionEvent.ACTION_UP ||
				action == MotionEvent.ACTION_DOWN) {
			int x = (int) event.getX();
			int y = (int) event.getY();

			x -= widget.getTotalPaddingLeft();
			y -= widget.getTotalPaddingTop();

			x += widget.getScrollX();
			y += widget.getScrollY();

			Layout layout = widget.getLayout();
			int line = layout.getLineForVertical(y);
			int off = layout.getOffsetForHorizontal(line, x);

			ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

			if (link.length != 0) {
				if (action == MotionEvent.ACTION_UP) {
					handleClick(widget, link[0]);
				} else if (action == MotionEvent.ACTION_DOWN) {
					Selection.setSelection(buffer,
							buffer.getSpanStart(link[0]),
							buffer.getSpanEnd(link[0]));
				}

				return true;
			} else {
				Selection.removeSelection(buffer);
			}
		}

		return super.onTouchEvent(widget, buffer, event);
	}

	@Override
	public void initialize(TextView widget, Spannable text)
	{
		Selection.removeSelection(text);
		text.removeSpan(FROM_BELOW);
	}

	@Override
	public void onTakeFocus(TextView view, Spannable text, int dir)
	{
		Selection.removeSelection(text);

		if ((dir & View.FOCUS_BACKWARD) != 0) {
			text.setSpan(FROM_BELOW, 0, 0, Spannable.SPAN_POINT_POINT);
		} else {
			text.removeSpan(FROM_BELOW);
		}
	}

	private void handleClick(TextView widget, ClickableSpan span)
	{
		boolean handled = false;

		if (mWordRefListener != null) {
			if (span instanceof URLSpan) {
				URLSpan urlSpan = (URLSpan) span;

				Matcher m = EXTRACT_REF.matcher(urlSpan.getURL());
				if (m.find()) {
					String protocol = m.group(1);
					int dir = Integer.parseInt(m.group(2));
					int number = Integer.parseInt(m.group(3));

					switch (protocol) {
					case PROTOCOL_REF:
						mWordRefListener.onWordReferenceSelected(dir, number);
						handled = true;
						break;
					case PROTOCOL_CITATION:
						mWordRefListener.onCitationRequested(dir, number);
						handled = true;
						break;
					}
				}
			}
		}

		if (!handled) {
			span.onClick(widget);
		}
	}

	public static String linkify(Crossword.Word word, String hint,
			Crossword crossword)
	{
		StringBuilder sb = new StringBuilder();
		if (hint == null) {
			hint = word.getHint();
		}

		if (word.getHintUrl() != null) {
			sb.append("<a href=\"")
					.append(word.getHintUrl())
					.append("\">")
					.append(TextUtils.htmlEncode(hint))
					.append("</a>");
		} else if (word.getCitation() != null) {
			sb.append(TextUtils.htmlEncode(hint))
					.append(" <a href=\"")
					.append(PROTOCOL_CITATION)
					.append("://")
					.append(word.getDirection())
					.append("/")
					.append(word.getNumber())
					.append("\">&#8224;</a>");
		} else {
			List<ReferenceScanner.WordReference> refs =
					ReferenceScanner.findReferences(hint, crossword);

			int start = 0;
			for (ReferenceScanner.WordReference ref: refs) {
				int refStart = ref.getStart();
				int refEnd = ref.getEnd();

				sb.append(TextUtils.htmlEncode(hint.substring(start, refStart)))
						.append("<a href=\"")
						.append(PROTOCOL_REF)
						.append("://")
						.append(ref.getDirection())
						.append("/")
						.append(ref.getNumber())
						.append("\">")
						.append(TextUtils.htmlEncode(hint.substring(refStart, refEnd)))
						.append("</a>");

				start = refEnd;
			}

			if (hint != null) {
				sb.append(TextUtils.htmlEncode(hint.substring(start)));
			}
		}

		return sb.toString();
	}

	private static Object FROM_BELOW = new NoCopySpan.Concrete();
}

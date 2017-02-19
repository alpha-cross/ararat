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

package org.akop.ararat;

import org.akop.ararat.core.Crossword;
import org.akop.ararat.io.CrosswordFormatter;
import org.akop.ararat.io.WSJFormatter;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertNotNull;


public class WSJFormatterUnitTest
		extends UnitTestBase
{
	@Test
	public void crossword_testParser()
			throws Exception
	{
		CrosswordFormatter formatter = new WSJFormatter();
		String url = "http://blogs.wsj.com/puzzle/crossword/20170217/22334/data.json";

		Crossword crossword = null;
		try {
			crossword = tryDownload(url, formatter);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}

		assertNotNull(crossword);

		System.out.println("title: " + crossword.getTitle());
		System.out.println("author: " + crossword.getAuthor());
		System.out.println("copyright: " + crossword.getCopyright());
		System.out.println("width: " + crossword.getWidth());
		System.out.println("height: " + crossword.getHeight());
		System.out.println("desc: " + crossword.getDescription());
		System.out.println("date: " + new Date(crossword.getDate()));

		System.out.println("across: " + crossword.getWordsAcross());
		System.out.println("down: " + crossword.getWordsDown());

		System.out.println("OK!");
	}
}
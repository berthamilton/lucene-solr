package org.apache.lucene.analysis.phonetic;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;

import org.apache.commons.codec.language.bm.NameType;
import org.apache.commons.codec.language.bm.PhoneticEngine;
import org.apache.commons.codec.language.bm.RuleType;
import org.apache.commons.codec.language.bm.Languages.LanguageSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.junit.Ignore;

/** Tests {@link BeiderMorseFilter} */
public class TestBeiderMorseFilter extends BaseTokenStreamTestCase {
  private Analyzer analyzer = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
      return new TokenStreamComponents(tokenizer, 
          new BeiderMorseFilter(tokenizer, new PhoneticEngine(NameType.GENERIC, RuleType.EXACT, true)));
    }
  };
  
  /** generic, "exact" configuration */
  public void testBasicUsage() throws Exception {    
    assertAnalyzesTo(analyzer, "Angelo",
        new String[] { "anZelo", "andZelo", "angelo", "anhelo", "anjelo", "anxelo" },
        new int[] { 0, 0, 0, 0, 0, 0 },
        new int[] { 6, 6, 6, 6, 6, 6 },
        new int[] { 1, 0, 0, 0, 0, 0 });
    
    assertAnalyzesTo(analyzer, "D'Angelo",
        new String[] { "anZelo", "andZelo", "angelo", "anhelo", "anjelo", "anxelo",
                  "danZelo", "dandZelo", "dangelo", "danhelo", "danjelo", "danxelo" },
        new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
        new int[] { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 },
        new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
  }
  
  /** restrict the output to a set of possible origin languages */
  public void testLanguageSet() throws Exception {
    final LanguageSet languages = LanguageSet.from(new HashSet<String>() {{
      add("italian"); add("greek"); add("spanish");
    }});
    Analyzer analyzer = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, 
            new BeiderMorseFilter(tokenizer, 
                new PhoneticEngine(NameType.GENERIC, RuleType.EXACT, true), languages));
      }
    };
    assertAnalyzesTo(analyzer, "Angelo",
        new String[] { "andZelo", "angelo", "anxelo" },
        new int[] { 0, 0, 0, },
        new int[] { 6, 6, 6, },
        new int[] { 1, 0, 0, });
  }
  
  /** for convenience, if the input yields no output, we pass it thru as-is */
  public void testNumbers() throws Exception {
    assertAnalyzesTo(analyzer, "1234",
        new String[] { "1234" },
        new int[] { 0 },
        new int[] { 4 },
        new int[] { 1 });
  }
  
  @Ignore("broken: causes OOM on some strings (https://issues.apache.org/jira/browse/CODEC-132)")
  public void testRandom() throws Exception {
    checkRandomData(random(), analyzer, 1000 * RANDOM_MULTIPLIER); 
  }
  
  public void testEmptyTerm() throws IOException {
    Analyzer a = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new KeywordTokenizer(reader);
        return new TokenStreamComponents(tokenizer, new BeiderMorseFilter(tokenizer, new PhoneticEngine(NameType.GENERIC, RuleType.EXACT, true)));
      }
    };
    checkOneTermReuse(a, "", "");
  }
}

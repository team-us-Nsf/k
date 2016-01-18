// Copyright (c) 2014-2016 K Team. All Rights Reserved.

package org.kframework.kore.convertors;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TestName;
import org.kframework.rewriter.Rewriter;
import org.kframework.attributes.Source;
import org.kframework.builtin.Sorts;
import org.kframework.definition.Module;
import org.kframework.kompile.CompiledDefinition;
import org.kframework.kompile.Kompile;
import org.kframework.kompile.KompileOptions;
import org.kframework.kore.K;
import org.kframework.main.GlobalOptions;
import org.kframework.tiny.FullTinyRewriter;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.BiFunction;


public class TstTinyOnKORE_IT {

    @org.junit.Rule
    public TestName name = new TestName();


    protected File testResource(String baseName) throws URISyntaxException {
        System.out.println(baseName);
        return new File(TstTinyOnKORE_IT.class.getResource(baseName).toURI());
    }

    @Test @Ignore
    public void kore_imp_tiny() throws IOException, URISyntaxException {
        executeTest("TEST", "TEST-PROGRAMS",
                "<top><k> while(0<=n) { s = s + n; n = n + -1; } </k><state>n|->10 s|->0</state></top>",
                "<top>(<k>(#KSequence()),<state>(_Map_(_|->_(n:Id,-1),_|->_(s:Id,55))))");
    }

    @Test @Ignore
    public void imp_lesson1() throws IOException, URISyntaxException {
        executeTest("IMP", "IMP-SYNTAX",
                "initKCell(`_|->_`($PGM, while(0<=n) { s = s + n; n = n + -1; }))",
                "<k>(while(_)_(_<=_(0,n:Id),{_}(__(_=_;(s:Id,_+_(s:Id,n:Id)),_=_;(n:Id,_+_(n:Id,-1))))))");
    }

    @Test @Ignore
    public void imp_lesson2() throws IOException, URISyntaxException {
        executeTest("IMP", "IMP-SYNTAX",
                "initTCell(`_|->_`($PGM, while(0<=n) { s = s + n; n = n + -1; }))",
                "<T>(<k>(while(_)_(_<=_(0,n:Id),{_}(__(_=_;(s:Id,_+_(s:Id,n:Id)),_=_;(n:Id,_+_(n:Id,-1)))))),<state>(_Map_()))");
    }

    @Test @Ignore("tiny backend does not work with domains.k")
    public void simpleNestedFunctions() throws IOException, URISyntaxException {
        executeTest("FUNC", "FUNC",
                "`foo`(bar)",
                "done()");
    }

    @Test @Ignore
    public void simpleNestedConfiguration() throws IOException, URISyntaxException {
        executeTest("FUNC", "FUNC",
                "initTopCell(.Map)",
                "<top><k>foo</k></top>");
    }

    private void executeTest(String mainModule, String mainSyntaxModule, String programText, String expected) throws URISyntaxException, IOException {
        String filename = "/convertor-tests/" + name.getMethodName() + ".k";

        File definitionFile = testResource(filename);
        KExceptionManager kem = new KExceptionManager(new GlobalOptions());
        try {
            CompiledDefinition compiledDef = new Kompile(new KompileOptions(), FileUtil.testFileUtil(), kem, false).run(definitionFile, mainModule, mainSyntaxModule);

            Module module = compiledDef.executionModule();
            BiFunction<String, Source, K> programParser = compiledDef.getProgramParser(kem);
            Rewriter rewriter = new FullTinyRewriter(module);


            K program = programParser.apply(programText, Source.apply("generated by " + getClass().getSimpleName()));

            long l = System.nanoTime();
            K result = rewriter.execute(program, Optional.<Integer>empty()).k();
            System.out.println("time = " + (System.nanoTime() - l) / 1000000);

            System.out.println("result = " + result.toString());

            Assert.assertEquals(expected, result.toString());
        } finally {
            kem.print();
        }
    }

}

/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */
package scouter.toys.bytescope.util;

import scouter.toys.bytescope.AgentMain;
import scouter.util.RequestQueue;
import scouter.util.SystemUtil;
import scouter.util.ThreadUtil;

import java.lang.instrument.ClassDefinition;

public class AsyncRunner extends Thread {

    private static AsyncRunner instance = null;
    private RequestQueue<Object> queue = new RequestQueue<Object>(1024);

    public final static synchronized AsyncRunner getInstance() {
        if (instance == null) {
            instance = new AsyncRunner();
            instance.setDaemon(true);
            instance.setName(ThreadUtil.getName(instance));
            instance.start();
        }
        return instance;
    }

    private static class Hook {
        public Hook(ClassLoader loader, String classname, byte[] body) {
            super();
            this.loader = loader;
            this.classname = classname.replace('/', '.');
            this.body = body;
        }
        ClassLoader loader;
        String classname;
        byte[] body;
    }

    public void add(ClassLoader loader, String classname, byte[] body) {
        queue.put(new Hook(loader, classname, body));
    }

    public void add(Runnable r) {
        queue.put(r);
    }

    public void run() {
        while (true) {
            Object m = queue.get(1000);
            try {
                if (m instanceof Hook) {
                    hooking((Hook) m);
                } else if (m instanceof Runnable) {
                    process((Runnable) m);
                }
            } catch (Throwable t) {
            	t.printStackTrace();
            }
        }
    }

    private void process(Runnable r) {
        r.run();
    }

    private void hooking(Hook m) {
        // Never use dynamic hooking on AIX with JDK1.5
        if (SystemUtil.IS_AIX && SystemUtil.IS_JAVA_1_5) {
            return;
        }
        try {
            Class cls = Class.forName(m.classname, false, m.loader);
            ClassDefinition[] cd = new ClassDefinition[1];
            cd[0] = new ClassDefinition(cls, m.body);
            AgentMain.getInstrumentation().redefineClasses(cd);
        } catch (Throwable t) {
            AgentLogger.println("A001", "async hook fail:" + m.classname + " " + t);
        }
    }
}

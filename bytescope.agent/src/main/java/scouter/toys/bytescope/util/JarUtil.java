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

import java.io.File;

public class JarUtil {

	public static File getThisJarFile() {
		String path = "";
		ClassLoader cl = AgentMain.class.getClassLoader();
		if (cl == null) {
			path = ""
					+ ClassLoader.getSystemClassLoader().getResource(AgentMain.class.getName().replace('.', '/') + ".class");
		} else {
			path = "" + cl.getResource(AgentMain.class.getName().replace('.', '/') + ".class");
		}
		try {
			path = path.substring("jar:file:/".length(), path.indexOf("!"));
			if (path.indexOf(':') > 0)
				return new File(path);
			else
				return new File("/" + path);
		} catch (Throwable th) {
			return null;
		}
	}
}
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

package scouter.toys.bytescope.asm.probe;

import scouter.org.objectweb.asm.ClassVisitor;
import scouter.org.objectweb.asm.MethodVisitor;
import scouter.org.objectweb.asm.Opcodes;
import scouter.org.objectweb.asm.Type;
import scouter.org.objectweb.asm.commons.LocalVariablesSorter;
import scouter.toys.bytescope.AgentConfigure;
import scouter.toys.bytescope.asm.AsmUtil;
import scouter.toys.bytescope.asm.ClassDesc;
import scouter.toys.bytescope.asm.HookingSet;
import scouter.toys.bytescope.asm.IASM;
import scouter.toys.bytescope.deco.CommonDeco;

import java.util.List;

/**
 * author Gunhlee(gunlee01@gmail.com)
 */
public class MethodBeforeProbe implements IASM, Opcodes {
	private AgentConfigure conf = AgentConfigure.getInstance();
	private List<HookingSet> target = HookingSet.getHookingMethodSet(conf.hook_injection_method_patterns);

	public ClassVisitor transform(ClassVisitor cv, String className, ClassDesc classDesc) {
		if (!conf.hook_enabled || !conf.hook_injection_method_enabled) {
			return cv;
		}

		for (int i = 0; i < target.size(); i++) {
			HookingSet mset = target.get(i);
			if (mset.classMatch.include(className)) {
				return new MethodInjectCV(cv, mset, className);
			}
		}
		return cv;
	}
}

// ///////////////////////////////////////////////////////////////////////////
class MethodInjectCV extends ClassVisitor implements Opcodes {

	public String className;
	private HookingSet mset;

	public MethodInjectCV(ClassVisitor cv, HookingSet mset, String className) {
		super(ASM4, cv);
		this.mset = mset;
		this.className = className;
	}

	@Override
	public MethodVisitor visitMethod(int access, String methodName, String desc, String signature,
			String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
		if (mv == null || mset.isA(methodName, desc) == false) {
			return mv;
		}
		if (AsmUtil.isSpecial(methodName)) {
			return mv;
		}
		return new MethodInjectMV(access, desc, mv, Type.getArgumentTypes(desc), (access & ACC_STATIC) != 0, className,
				methodName, desc);
	}
}

// ///////////////////////////////////////////////////////////////////////////
class MethodInjectMV extends LocalVariablesSorter implements Opcodes {
	private static final String CLASS = CommonDeco.class.getName().replace('.', '/');
	private static final String METHOD = "before";
	private static final String SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)V";

	private Type[] paramTypes;
	private boolean isStatic;
	private String className;
	private String methodName;
	private String methodDesc;

	public MethodInjectMV(int access, String desc, MethodVisitor mv, Type[] paramTypes, boolean isStatic, String classname,
						  String methodname, String methoddesc) {
		super(ASM4, access, desc, mv);
		this.paramTypes = paramTypes;
		this.isStatic = isStatic;
		this.className = classname;
		this.methodName = methodname;
		this.methodDesc = methoddesc;

	}

	@Override
	public void visitCode() {

		int sidx = isStatic ? 0 : 1;

		int arrVarIdx = newLocal(Type.getType("[Ljava/lang/Object;"));
		AsmUtil.PUSH(mv, paramTypes.length);
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
		mv.visitVarInsn(Opcodes.ASTORE, arrVarIdx);

		for (int i = 0; i < paramTypes.length; i++) {
			Type type = paramTypes[i];
			mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);
			AsmUtil.PUSH(mv, i);

			switch (type.getSort()) {
			case Type.BOOLEAN:
				mv.visitVarInsn(Opcodes.ILOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",
						false);
				break;
			case Type.BYTE:
				mv.visitVarInsn(Opcodes.ILOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
				break;
			case Type.CHAR:
				mv.visitVarInsn(Opcodes.ILOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;",
						false);
				break;
			case Type.SHORT:
				mv.visitVarInsn(Opcodes.ILOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
				break;
			case Type.INT:
				mv.visitVarInsn(Opcodes.ILOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;",
						false);
				break;
			case Type.LONG:
				mv.visitVarInsn(Opcodes.LLOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
				break;
			case Type.FLOAT:
				mv.visitVarInsn(Opcodes.FLOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
				break;
			case Type.DOUBLE:
				mv.visitVarInsn(Opcodes.DLOAD, sidx);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
				break;
			default:
				mv.visitVarInsn(Opcodes.ALOAD, sidx);
			}
			mv.visitInsn(Opcodes.AASTORE);
			sidx += type.getSize();
		}
		AsmUtil.PUSH(mv, className);
		AsmUtil.PUSH(mv, methodName);
		AsmUtil.PUSH(mv, methodDesc);
		if (isStatic) {
			AsmUtil.PUSHNULL(mv);
		} else {
			mv.visitVarInsn(Opcodes.ALOAD, 0);
		}
		mv.visitVarInsn(Opcodes.ALOAD, arrVarIdx);

		mv.visitMethodInsn(Opcodes.INVOKESTATIC, CLASS, METHOD, SIGNATURE, false);
		mv.visitCode();
	}
}
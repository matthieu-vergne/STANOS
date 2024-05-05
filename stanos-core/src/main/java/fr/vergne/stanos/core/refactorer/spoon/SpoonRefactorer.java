package fr.vergne.stanos.core.refactorer.spoon;

import java.util.stream.Stream;

import fr.vergne.stanos.core.refactorer.Code;
import fr.vergne.stanos.core.refactorer.Component;
import fr.vergne.stanos.core.refactorer.Component.Class;
import fr.vergne.stanos.core.refactorer.Component.Field;
import fr.vergne.stanos.core.refactorer.Component.Interface;
import fr.vergne.stanos.core.refactorer.Component.Method;
import fr.vergne.stanos.core.refactorer.Component.Package;
import fr.vergne.stanos.core.refactorer.Component.Record;
import fr.vergne.stanos.core.refactorer.Refactorer;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.DefaultTokenWriter;
import spoon.reflect.visitor.PrettyPrinter;
import spoon.support.compiler.VirtualFile;
import spoon.support.sniper.SniperJavaPrettyPrinter;

public interface SpoonRefactorer extends Refactorer {

	static Refactorer.ForCode forCode(String code) {
		Launcher launcher = new Launcher();
		launcher.addInputResource(new VirtualFile(code));
		CtModel model = launcher.buildModel();
		launcher.getEnvironment().setPrettyPrinterCreator(() -> {
			return new SniperJavaPrettyPrinter(launcher.getEnvironment());
		});

		return new Refactorer.ForCode() {

			@Override
			public String code() {
				return model.getRootPackage().getTypes().iterator().next().toString();
			}

			@Override
			public Code.Source source() {
				return new Code.Source() {

					@Override
					public Package defaultPackage() {
						CtPackage spoonPackage = model.getRootPackage();
						return new Component.Package() {

							@Override
							public void rename(String newName) {
								// TODO Auto-generated method stub
								throw new UnsupportedOperationException("Not implemented yet");
							}

							@Override
							public Stream<Record> records() {
								// TODO Auto-generated method stub
								throw new UnsupportedOperationException("Not implemented yet");
							}

							@Override
							public String name() {
								// TODO Auto-generated method stub
								throw new UnsupportedOperationException("Not implemented yet");
							}

							@Override
							public Stream<Interface> interfaces() {
								// TODO Auto-generated method stub
								throw new UnsupportedOperationException("Not implemented yet");
							}

							@Override
							public Stream<Class> classes() {
								return spoonPackage.getTypes().stream().map(spoonType -> {
									return new Component.Class() {

										@Override
										public void rename(String newName) {
											spoonType.setSimpleName(newName);
										}

										@Override
										public String name() {
											return spoonType.getSimpleName();
										}

										@Override
										public Stream<Method> methods() {
											// TODO Auto-generated method stub
											throw new UnsupportedOperationException("Not implemented yet");
										}

										@Override
										public Stream<Interface> interfaces() {
											// TODO Auto-generated method stub
											throw new UnsupportedOperationException("Not implemented yet");
										}

										@Override
										public Stream<Field> fields() {
											// TODO Auto-generated method stub
											throw new UnsupportedOperationException("Not implemented yet");
										}

										@Override
										public Stream<Class> classes() {
											// TODO Auto-generated method stub
											throw new UnsupportedOperationException("Not implemented yet");
										}
									};
								});
							}
						};
					}
				};
			}
		};
	}
}

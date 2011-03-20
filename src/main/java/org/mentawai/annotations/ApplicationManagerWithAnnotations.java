/*
 * Copyright (c) 2006 by UNESP - Universidade Estadual Paulista "JÚLIO DE MESQUITA FILHO"
 *               Faculdade de Ciências, Bauru, São Paulo, Brasil
 *               http://www.fc.unesp.br, http://www.unesp.br
 *
 *
 * Este arquivo é parte do programa DocFlow.
 *
 * DocFlow é um software livre; você pode redistribui-lo e/ou
 * modifica-lo dentro dos termos da Licença Pública Geral GNU como
 * publicada pela Fundação do Software Livre (FSF); na versão 2 da
 * Licença, ou (na sua opnião) qualquer versão.
 *
 * DocFlow é distribuido na esperança que possa ser util,
 * mas SEM NENHUMA GARANTIA; sem uma garantia implicita de ADEQUAÇÂO a qualquer
 * MERCADO ou APLICAÇÃO EM PARTICULAR. Veja a
 * Licença Pública Geral GNU para maiores detalhes.
 *
 * Você deve ter recebido uma cópia da Licença Pública Geral GNU
 * junto com DocFlow, se não, escreva para a Fundação do Software
 * Livre(FSF) Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *
 * This file is part of DocFlow.
 *
 * DocFlow is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * DocFlow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DocFlow; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA *
 *
 *
 * Date:    06/03/2008 15:37:33
 *
 * Author:  André Penteado
 */

package org.mentawai.annotations;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.mentawai.ajax.AjaxConsequence;
import org.mentawai.ajax.AjaxRenderer;
import org.mentawai.ajax.renderer.MapAjaxRenderer;
import org.mentawai.annotations.core.ActionClass;
import org.mentawai.annotations.core.ConsequenceOutput;
import org.mentawai.annotations.core.Consequences;
import org.mentawai.annotations.type.ConsequenceType;
import org.mentawai.core.ActionConfig;
import org.mentawai.core.ApplicationManager;
import org.mentawai.core.BaseAction;
import org.mentawai.core.Chain;
import org.mentawai.core.Consequence;
import org.mentawai.core.Forward;
import org.mentawai.core.Redirect;
import org.mentawai.core.StreamConsequence;

/**
 * @author Andre Penteado
 * @author Fernando Boaglio
 *
 * @since 06/03/2008 - 15:37:33
 */
public abstract class ApplicationManagerWithAnnotations extends	ApplicationManager {

	private static String resources;

	@SuppressWarnings("rawtypes")
	private HashSet<Class> annotatedClasses = new HashSet<Class>();

	@Override
	public void loadActions() {
		super.loadActions();
		loadAnnotatedClasses();
	}

	private static  Pattern getPackageTestJar() {
		return Pattern.compile(resources+"/[A-Za-z0-9/]*actions.*");
	}

	private static Pattern getPackageTest() {
		return  Pattern.compile(".*["
				+ StringEscapeUtils.escapeJava(File.separator) + "]("+resources+"["
				+ StringEscapeUtils.escapeJava(File.separator) + "][A-Za-z0-9"
				+ StringEscapeUtils.escapeJava(File.separator) + "]*actions.*)");

	}

	@SuppressWarnings("rawtypes")
	private void findAnnotatedClass(File file) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				findAnnotatedClass(f);
			}
		} else {
			Matcher m = getPackageTest().matcher(file.getAbsolutePath());
			if (m.matches() && file.getName().endsWith(".class")) {
				String className = m.group(1);
				className = className.replaceAll(
						StringEscapeUtils.escapeJava(File.separator), ".");
				className = className.substring(0, className.length() - 6);
				try {
					Class klass = Class.forName(className);
					if (BaseAction.class.isAssignableFrom(klass)) {
						System.out.println("Add Annotated Class:" + className);
						annotatedClasses.add(klass);
					}
				} catch (Exception e) {
					System.out.println("error! this class was not started:"+ className);
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void findAnnotatedClasses(String resources) throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Enumeration<URL> urls = cl.getResources(resources);
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			if (url.getProtocol().equals("file")) {
				File file = new File(url.getFile());
				findAnnotatedClass(file);
			} else if (url.getProtocol().equals("jar")) {
				URL urlJar = new URL(url.getFile().split("\\!")[0]);
				JarFile jarFile;
				try {
					jarFile = new JarFile(new File(urlJar.toURI().getPath()));
				} catch (URISyntaxException ex) {
					Logger.getLogger(
							ApplicationManagerWithAnnotations.class.getName())
							.log(Level.SEVERE, null, ex);
					continue;
				}
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (getPackageTestJar().matcher(entry.getName()).matches()
							&& entry.getName().endsWith(".class")) {
						String className = entry.getName().replace('/', '.')
								.substring(0, entry.getName().length() - 6);
						try {
							Class klass = Class.forName(className);
							if (BaseAction.class.isAssignableFrom(klass)) {
								System.out.println("Add Annotated Class:"
										+ className);
								annotatedClasses.add(klass);
							}
						} catch (Exception e) {
							System.out
									.println("error! this class was not started:"+ entry.getName());
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public void loadAnnotatedClasses() {
		ActionConfig ac;
		ArrayList<ActionConfig> acListForChains = new ArrayList<ActionConfig>();

		// Para carregamento das Chains
		ArrayList<String> actionName = new ArrayList<String>();
		ArrayList<String> innerActionName = new ArrayList<String>();
		ArrayList<String> actionChain = new ArrayList<String>();
		ArrayList<String> innerChain = new ArrayList<String>();

		if (resources == null) {
			System.out.println("NO RESOURCES DEFINED");
			return;
		}

		try {
			System.out.println("Searching for annotated classes inside package ["+resources+"]...");
			findAnnotatedClasses(resources);
		} catch (Exception e) {
			System.out.println("COULD NOT LOAD PACKAGE ANNOTATED CLASSES: ");
			e.printStackTrace();
			return;
		}
		HashSet<Class> annotatedRemove = new HashSet<Class>();
		for (Class<? extends Object> klass : annotatedClasses) {
			if (klass.getSuperclass().isAnnotationPresent(ActionClass.class)) {
				annotatedRemove.add(klass.getSuperclass());
				System.out.println("REMOVING "
								+ klass.getSuperclass()
								+ ": this actions will be mapped by its subclass "
								+ klass.getName() + ".");
			}
		}
		for (Class<? extends Object> klass2Remove : annotatedRemove) {
			annotatedClasses.remove(klass2Remove);
		}

		if (annotatedClasses.size() > 0) {
			for (Class<? extends Object> klass : annotatedClasses) {
				if (klass.isAnnotationPresent(ActionClass.class)) {
					System.out.println("LOADING ANNOTATED ACTION CLASS: "
							+ klass.getName());

					// Mapear o prefixo da ação com a classe de ação
					ActionClass annotation = klass
							.getAnnotation(ActionClass.class);
					if (getActions().containsKey(annotation.prefix())) {
						ac = getActions().get(annotation.prefix());
						if (ac.getActionClass().isAssignableFrom(klass)) {
							ac = new ActionConfig(annotation.prefix(), klass);
						}
					} else
						ac = new ActionConfig(annotation.prefix(), klass);

					// Mapear as consequencias default (sem método de ação) da
					// classe
					ConsequenceOutput[] outputsClass = annotation.outputs();
					if (outputsClass != null && outputsClass.length > 0) {
						for (ConsequenceOutput output : outputsClass) {
							if (output.type() == ConsequenceType.FORWARD) {
								// Caso seja a consequencia padrão, mapear
								// sucesso e erro para a mesma página
								if (ConsequenceOutput.SUCCESS_ERROR
										.equals(output.result())) {
									ac.addConsequence(SUCCESS, new Forward(
											output.page()));
									ac.addConsequence(ERROR,
											new Forward(output.page()));
								} else
									ac.addConsequence(output.result(),
											new Forward(output.page()));
							} else if (output.type() == ConsequenceType.REDIRECT) {
								ac.addConsequence(
										output.result(),
										"".equals(output.page()) ? new Redirect(
												output.RedirectWithParameters())
												: new Redirect(
														output.page(),
														output.RedirectWithParameters()));
							} else if (output.type() == ConsequenceType.STREAMCONSEQUENCE) {
								ac.addConsequence(
										output.result(),
										"".equals(output.page()) ? new StreamConsequence()
												: new StreamConsequence(output
														.page()));
							} else if (output.type() == ConsequenceType.AJAXCONSEQUENCE) {
								try {
									AjaxRenderer ajaxRender = (AjaxRenderer) Class
											.forName(output.page())
											.newInstance();
									ac.addConsequence(output.result(),
											new AjaxConsequence(ajaxRender));
								} catch (InstantiationException ex) {
									ac.addConsequence(output.result(),
											new AjaxConsequence(
													new MapAjaxRenderer()));
								} catch (Exception ex) {
									System.out
											.println("COULD NOT LOAD AJAX CONSEQUENCE, LOADED DEFAULT MapAjaxRenderer");
									ex.printStackTrace();
								}
							} else if (output.type() == ConsequenceType.CUSTOM) {
								try {
									Consequence customObject = (Consequence) Class
											.forName(output.page())
											.newInstance();
									ac.addConsequence(output.result(),
											customObject);
								} catch (Exception ex) {
									System.out
											.println("COULD NOT LOAD CUSTOM CONSEQUENCE: ACTION NOT LOADED: "
													+ klass.getSimpleName());
									ex.printStackTrace();
								}
							}
						}
					}

					// Mapear os métodos de ações da classe
					for (Method method : klass.getMethods()) {
						if (method.isAnnotationPresent(Consequences.class)) {
							System.out.println("LOADING CONSEQUENCE: "
									+ annotation.prefix() + "."
									+ method.getName());

							// Buscar as consequencias anotadas na classe de
							// ação
							ConsequenceOutput[] mapConsequences = method
									.getAnnotation(Consequences.class)
									.outputs();

							// Mapeando as consequencias
							if (mapConsequences != null
									&& mapConsequences.length > 0) {
								for (ConsequenceOutput output : mapConsequences) {
									if (output.type() == ConsequenceType.FORWARD) {
										// Caso seja a consequencia padrão,
										// mapear sucesso e erro para a mesma
										// página
										if (ConsequenceOutput.SUCCESS_ERROR
												.equals(output.result())) {
											ac.addConsequence(SUCCESS,
													method.getName(),
													new Forward(output.page()));
											ac.addConsequence(ERROR,
													method.getName(),
													new Forward(output.page()));
										} else
											ac.addConsequence(output.result(),
													method.getName(),
													new Forward(output.page()));
									} else if (output.type() == ConsequenceType.REDIRECT) {
										Consequence c = null;
										if (output.page().equals("")) {
											c = new Redirect(
													output.RedirectWithParameters());
										} else {
											c = new Redirect(
													output.page(),
													output.RedirectWithParameters());
										}
										if (ConsequenceOutput.SUCCESS_ERROR
												.equals(output.result())) {
											ac.addConsequence(SUCCESS,
													method.getName(), c);
											ac.addConsequence(ERROR,
													method.getName(), c);
										} else {
											ac.addConsequence(output.result(),
													method.getName(), c);
										}
									} else if (output.type() == ConsequenceType.STREAMCONSEQUENCE) {
										ac.addConsequence(
												output.result(),
												method.getName(),
												"".equals(output.page()) ? new StreamConsequence()
														: new StreamConsequence(
																output.page()));
									} else if (output.type() == ConsequenceType.AJAXCONSEQUENCE) {
										try {
											AjaxRenderer ajaxRender = (AjaxRenderer) Class
													.forName(output.page())
													.newInstance();
											ac.addConsequence(output.result(),
													method.getName(),
													new AjaxConsequence(
															ajaxRender));
										} catch (InstantiationException ex) {
											ac.addConsequence(
													output.result(),
													method.getName(),
													new AjaxConsequence(
															new MapAjaxRenderer()));
										} catch (Exception ex) {
											System.out
													.println("COULD NOT LOAD AJAX CONSEQUENCE, LOADED DEFAULT MapAjaxRenderer");
											ex.printStackTrace();
										}
									} else if (output.type() == ConsequenceType.CUSTOM) {
										try {
											Consequence customObject = (Consequence) Class
													.forName(output.page())
													.newInstance();
											ac.addConsequence(output.result(),
													method.getName(),
													customObject);
										} catch (Exception ex) {
											System.out
													.println("COULD NOT LOAD CUSTOM CONSEQUENCE: ACTION NOT LOADED: "
															+ klass.getSimpleName()
															+ "."
															+ method.getName());
											ex.printStackTrace();
										}
									} else if (output.type() == ConsequenceType.CHAIN) {
										String[] explodedParameter = output
												.page().split("\\."); // Usado
																		// para
																		// explodir
																		// parameters
																		// no
																		// caso
																		// de
																		// uma
																		// chain
										actionName.add(output.result());
										innerActionName.add(method.getName());
										actionChain.add(explodedParameter[0]);
										innerChain
												.add(explodedParameter.length > 1 ? explodedParameter[1]
														: null);
										acListForChains.add(ac);
									}
								}
							}
						}
					}
					add(ac);
				}
			}
		}

		// Carrega as chains atrasadas porque os ActionConfigs podem ainda nao
		// ter sido carregados
		int length = actionName.size();
		Chain chain;
		for (int i = 0; i < length; i++) {
			try {
				ac = acListForChains.get(i);
				if (innerChain.get(i) == null)
					chain = new Chain(getActionConfig(actionChain.get(i)));
				else
					chain = new Chain(getActionConfig(actionChain.get(i)),
							innerChain.get(i));
				if (actionName.get(i).equals(ConsequenceOutput.SUCCESS_ERROR)) {
					ac.addConsequence(SUCCESS, innerActionName.get(i), chain);
					ac.addConsequence(ERROR, innerActionName.get(i), chain);
				} else {
					ac.addConsequence(actionName.get(i),
							innerActionName.get(i), chain);
				}

			} catch (Exception e) {
				System.out
						.println("COULD NOT LOAD CHAIN CONSEQUENCE: ACTION NOT LOADED: "
								+ actionChain.get(i)
								+ "."
								+ innerActionName.get(i));
			}
		}
	}

	public void setResources(String resources) {
		ApplicationManagerWithAnnotations.resources = resources;
	}

}
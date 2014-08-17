/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cuchaz.enigma.analysis.Ancestries;
import cuchaz.enigma.mapping.SignatureUpdater.ClassNameUpdater;

public class Translator
{
	private TranslationDirection m_direction;
	public Map<String,ClassMapping> m_classes;
	private Ancestries m_ancestries;
	
	protected Translator( TranslationDirection direction, Map<String,ClassMapping> classes, Ancestries ancestries )
	{
		m_direction = direction;
		m_classes = classes;
		m_ancestries = ancestries;
	}
	
	public String translateClass( String className )
	{
		return translate( new ClassEntry( className ) );
	}
	
	public String translate( ClassEntry in )
	{
		ClassMapping classMapping = m_classes.get( in.getOuterClassName() );
		if( classMapping != null )
		{
			if( in.isInnerClass() )
			{
				// look for the inner class
				String translatedInnerClassName = m_direction.choose(
					classMapping.getDeobfInnerClassName( in.getInnerClassName() ),
					classMapping.getObfInnerClassName( in.getInnerClassName() )
				);
				if( translatedInnerClassName != null )
				{
					// return outer$inner
					String translatedOuterClassName = m_direction.choose(
						classMapping.getDeobfName(),
						classMapping.getObfName()
					);
					return translatedOuterClassName + "$" + translatedInnerClassName;
				}
			}
			else
			{
				// just return outer
				return m_direction.choose(
					classMapping.getDeobfName(),
					classMapping.getObfName()
				);
			}
		}
		return null;
	}
	
	public ClassEntry translateEntry( ClassEntry in )
	{
		String name = translate( in );
		if( name == null )
		{
			return in;
		}
		return new ClassEntry( name );
	}
	
	public String translate( FieldEntry in )
	{
		for( String className : getSelfAndAncestors( in.getClassName() ) )
		{
			// look for the class
			ClassMapping classMapping = findClassMapping( new ClassEntry( className ) );
			if( classMapping != null )
			{
				// look for the field
				String translatedName = m_direction.choose(
					classMapping.getDeobfFieldName( in.getName() ),
					classMapping.getObfFieldName( in.getName() )
				);
				if( translatedName != null )
				{
					return translatedName;
				}
			}
		}
		return null;
	}
	
	public FieldEntry translateEntry( FieldEntry in )
	{
		String name = translate( in );
		if( name == null )
		{
			name = in.getName();
		}
		return new FieldEntry(
			translateEntry( in.getClassEntry() ),
			name
		);
	}
	
	public String translate( MethodEntry in )
	{
		for( String className : getSelfAndAncestors( in.getClassName() ) )
		{
			// look for class
			ClassMapping classMapping = findClassMapping( new ClassEntry( className ) );
			if( classMapping != null )
			{
				// look for the method
				MethodMapping methodMapping = m_direction.choose(
					classMapping.getMethodByObf( in.getName(), in.getSignature() ),
					classMapping.getMethodByDeobf( in.getName(), in.getSignature() )
				);
				if( methodMapping != null )
				{
					return m_direction.choose(
						methodMapping.getDeobfName(),
						methodMapping.getObfName()
					);
				}
			}
		}
		
		return null;
	}
	
	public MethodEntry translateEntry( MethodEntry in )
	{
		String name = translate( in );
		if( name == null )
		{
			name = in.getName();
		}
		return new MethodEntry(
			translateEntry( in.getClassEntry() ),
			name,
			translateSignature( in.getSignature() )
		);
	}
	
	public ConstructorEntry translateEntry( ConstructorEntry in )
	{
		return new ConstructorEntry(
			translateEntry( in.getClassEntry() ),
			translateSignature( in.getSignature() )
		);
	}
	
	public String translate( ArgumentEntry in )
	{
		for( String className : getSelfAndAncestors( in.getClassName() ) )
		{
			// look for the class
			ClassMapping classMapping = findClassMapping( new ClassEntry( className ) );
			if( classMapping != null )
			{
				// look for the method
				MethodMapping methodMapping = m_direction.choose(
					classMapping.getMethodByObf( in.getMethodName(), in.getMethodSignature() ),
					classMapping.getMethodByDeobf( in.getMethodName(), in.getMethodSignature() )
				);
				if( methodMapping != null )
				{
					return m_direction.choose(
						methodMapping.getDeobfArgumentName( in.getIndex() ),
						methodMapping.getObfArgumentName( in.getIndex() )
					);
				}
			}
		}
		
		return null;
	}
	
	public ArgumentEntry translateEntry( ArgumentEntry in )
	{
		String name = translate( in );
		if( name == null )
		{
			name = in.getName();
		}
		return new ArgumentEntry(
			translateEntry( in.getMethodEntry() ),
			in.getIndex(),
			name
		);
	}
	
	public String translateSignature( String signature )
	{
		return SignatureUpdater.update( signature, new ClassNameUpdater( )
		{
			@Override
			public String update( String className )
			{
				String translatedName = translateClass( className );
				if( translatedName != null )
				{
					return translatedName;
				}
				return className;
			}
		} );
	}
	
	private List<String> getSelfAndAncestors( String className )
	{
		List<String> ancestry = new ArrayList<String>();
		ancestry.add( className );
		ancestry.addAll( m_ancestries.getAncestry( className ) );
		return ancestry;
	}
	
	private ClassMapping findClassMapping( ClassEntry classEntry )
	{
		ClassMapping classMapping = m_classes.get( classEntry.getOuterClassName() );
		if( classMapping != null && classEntry.isInnerClass() )
		{
			classMapping = m_direction.choose(
				classMapping.getInnerClassByObf( classEntry.getInnerClassName() ),
				classMapping.getInnerClassByDeobf( classEntry.getInnerClassName() )
			);
		}
		return classMapping;
	}
}

package io.xol.chunkstories.api.plugin.commands;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Command
{
	private String name;

	public Command(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof String)
			return ((String)o).equals(name);
		return ((Command) o).name.equals(name);
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
}
#!/usr/bin/perl -w


usage("Wrong number of args") unless(@ARGV==2);
$x_offset = shift;
$y_offset = shift;

#--- 
#- 
#    id: 00:00:00:12:e2:78:31:f5
#    x: 201
#    y: 344
#- 
#    id: 00:00:00:12:e2:98:a5:d3
#    x: 29
#    y: 349
#- 

while(<>)
{
	if(/^(\s*x): (\d+)/)
	{
		print $1,": ",$2 + $x_offset, "\n";
	}
	elsif(/^(\s*y): (\d+)/)
	{
		print $1,": ",$2 + $y_offset, "\n";
	}
	else 
	{
		print;
	}
}


sub usage
{
	my ($str)=@_;

	print STDERR "$str\n";
	print STDERR << "EOF";
Usage:

dieyamldie.pl xoff yoff  < in.yaml >  out.yaml

EOF
	exit(1);

}

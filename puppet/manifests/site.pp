$app_name = "stock-feed-demo"

Exec { path => [ "/bin/", "/sbin/" , "/usr/bin/", "/usr/sbin/" ] }

package { "openjdk-7-jdk":
	ensure => latest
}

class {"play":
	version => "2.0.4",
	apps_user => "www-data",
	apps_group => "www-data",
}

class { "stock-feed-demo":
	require => Class["Play"],
}

package { "lighttpd-remove":
	name => "lighttpd",
	ensure => "purged",
}

stock-feed-demo::nginx { "$app_name":
	require => [Class["stock-feed-demo"], Package["lighttpd-remove"] ]
}

class { "gitapp":
	app_name	=> "$app_name",
	repository  => "https://github.com/gorillabuilderz/stock-feeder-demo.git",
	path		=> "$play::apps_home",
	owner 		=> "$play::apps_user",
	group 		=> "$play::apps_group",
	require => Class["Play"],
}

play::clean { "$app_name":
	subscribe => Class["gitapp"]
}

play::service { "$app_name":
	require => [
			Class["Play"], 
			Package["openjdk-7-jdk"],
			Class["gitapp"]
		],
}

play::stage { "$app_name":
	subscribe => [Class["$app_name"], Play::Clean["$app_name"]],
	require => [Package["openjdk-7-jdk"], Class["gitapp"]],
}

service { "$app_name":
	ensure => running,
	enable => true,
    subscribe  => [Play::Stage["$app_name"], Class["gitapp"]],	
	require => [ 
			Play::Service["$app_name"], 
			Class["$app_name"], 
			Play::Stage["$app_name"], 
			Package["openjdk-7-jdk"] 
		],
}

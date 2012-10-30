define stock-feed-demo::nginx {
	exec { "nginx-add-apt-repository":
		command => "add-apt-repository -y ppa:nginx/stable",
		unless => "grep -R nginx /etc/apt/* 2>/dev/null"
	}

	exec { "nginx-apt-get-update":
		command => "apt-get update",
		require => Exec["nginx-add-apt-repository"]
	}

	package { "nginx":
		ensure => latest,
		require => Exec["nginx-apt-get-update"]
	}
	
	service { "nginx":
		enable => true,
	    ensure => running,
	    require => Package[nginx],
	}
	
	file { "/etc/nginx/sites-available/stock-feed-demo":
		owner   => "root",
		group   => "root",
		mode    => "0644",
		source  => "puppet:///modules/stock-feed-demo/etc/nginx/sites-available/stock-feed-demo",
		require => Package["nginx"],
	}
	
	file { "/etc/nginx/sites-enabled/stock-feed-demo":
		ensure  => link,
		target => "/etc/nginx/sites-available/stock-feed-demo",
		require => File["/etc/nginx/sites-available/stock-feed-demo"],
		notify  => Service["nginx"],
	}	

	file { "/etc/nginx/sites-enabled/default":
		ensure  => absent
	}	
}
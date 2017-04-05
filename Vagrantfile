# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version.
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|

  config.ssh.username = "vagrant"
  config.ssh.password = "vagrant"

  # Services to be used by Microservices.
  config.vm.define "services" do |server|

    server.vm.box = "williamyeh/ubuntu-trusty64-docker"

    server.vm.provider "virtualbox" do |v|
      v.memory = 2048
      v.cpus = 2
    end

    server.vm.network "private_network", ip: "192.168.1.10"

    server.vm.provision "docker" do |d|
      d.run "progrium/consul",
            args: "-d --restart always -p 8300:8300 -p 8301:8301 -p 8301:8301/udp -p 8302:8302 -p 8302:8302/udp -p 8400:8400 -p 8500:8500 --name consul-server",
            cmd: "-server -bootstrap -ui-dir /ui -advertise 192.168.1.10"
    end

  end

end

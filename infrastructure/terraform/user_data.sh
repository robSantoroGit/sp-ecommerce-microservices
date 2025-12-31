#!/bin/bash

# Update system
apt-get update
apt-get upgrade -y

# Set hostname
hostnamectl set-hostname ecommerce-k3s

# Install dependencies
apt-get install -y curl wget git apt-transport-https ca-certificates software-properties-common

# Install Docker (per build images se necessario)
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
usermod -aG docker ubuntu

# Install K3s
curl -sfL https://get.k3s.io | sh -s - \
  --write-kubeconfig-mode 644 \
  --disable traefik \
  --node-name ecommerce-k3s

# Wait for K3s to be ready
sleep 30

# Create .kube directory for ubuntu user
mkdir -p /home/ubuntu/.kube
cp /etc/rancher/k3s/k3s.yaml /home/ubuntu/.kube/config
chown -R ubuntu:ubuntu /home/ubuntu/.kube
chmod 600 /home/ubuntu/.kube/config

# Install kubectl alias
echo "alias kubectl='k3s kubectl'" >> /home/ubuntu/.bashrc

# Install helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Install AWS CLI (per ECR login)
apt-get install -y awscli

# Log completion
echo "K3s installation completed at $(date)" > /var/log/user-data-complete.log
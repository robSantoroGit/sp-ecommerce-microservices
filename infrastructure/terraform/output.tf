output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "ec2_instance_id" {
  description = "EC2 Instance ID"
  value       = aws_instance.k3s.id
}

output "ec2_public_ip" {
  description = "EC2 Public IP (Elastic IP)"
  value       = aws_eip.k3s.public_ip
}

output "ssh_command" {
  description = "SSH command per connettersi"
  value       = "ssh -i k3s-key ubuntu@${aws_eip.k3s.public_ip}"
}

output "k3s_api_endpoint" {
  description = "K3s API endpoint"
  value       = "https://${aws_eip.k3s.public_ip}:6443"
}

output "kubeconfig_command" {
  description = "Comando per copiare kubeconfig"
  value       = "scp -i k3s-key ubuntu@${aws_eip.k3s.public_ip}:/home/ubuntu/.kube/config ~/.kube/config-aws"
}

output "api_url" {
  description = "URL API Gateway (dopo deploy K8s)"
  value       = "http://${aws_eip.k3s.public_ip}:30080"
}
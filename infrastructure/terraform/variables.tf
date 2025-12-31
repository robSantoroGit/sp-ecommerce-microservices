variable "aws_region" {
  description = "AWS region dove creare le risorse"
  type        = string
  default     = "eu-south-1"
}

variable "environment" {
  description = "Environment name (dev, staging, production)"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Nome progetto"
  type        = string
  default     = "ecommerce"
}

variable "instance_type" {
  description = "EC2 instance type per K3s"
  type        = string
  default     = "t3.small"
}

variable "my_ip" {
  description = "Tuo IP pubblico per SSH (formato: x.x.x.x/32)"
  type        = string
}
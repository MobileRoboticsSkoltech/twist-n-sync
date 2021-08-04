//
// Created by achains on 26.07.2021.
//

#ifndef TWIST_N_SYNC_CPP_MODULE_CUBICSPLINE_H
#define TWIST_N_SYNC_CPP_MODULE_CUBICSPLINE_H

#include "spline.h"
#include "Eigen/Core"

class CubicSpline {
 public:
    explicit CubicSpline(Eigen::VectorXd const & X, Eigen::VectorXd const & Y);

    Eigen::Matrix4Xd getCoefficients();

    Eigen::VectorXd getValuesOnSegment(Eigen::VectorXd const &);

    double operator() (double const & x);

 private:
    void calculateDerivative();

    tk::spline m_spline_;
    std::vector<double> derivative_;
};


#endif //TWIST_N_SYNC_CPP_MODULE_CUBICSPLINE_H
